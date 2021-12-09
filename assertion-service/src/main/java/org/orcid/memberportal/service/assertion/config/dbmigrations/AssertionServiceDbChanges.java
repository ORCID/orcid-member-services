package org.orcid.memberportal.service.assertion.config.dbmigrations;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;

@ChangeLog(order = "001")
public class AssertionServiceDbChanges {
   
    private static final Logger LOG = LoggerFactory.getLogger(AssertionServiceDbChanges.class);

    @ChangeSet(order = "01", author = "George Nash", id = "01-populateLastSyncAttempts")
    public void addAuthorities(MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("last_sync_attempt").exists(false));
        List<Assertion> assertionsWithEmptyLastSyncAttempt = mongoTemplate.find(query, Assertion.class, "assertion");
        assertionsWithEmptyLastSyncAttempt.forEach(a -> {
            if (a.getUpdatedInORCID() != null) {
                a.setLastSyncAttempt(a.getUpdatedInORCID());
                mongoTemplate.save(a);
            } else if (a.getAddedToORCID() != null) {
                a.setLastSyncAttempt(a.getAddedToORCID());
                mongoTemplate.save(a);
            }
        });
    }
    
    @ChangeSet(order = "02", author = "George Nash", id = "02-convertAffiliationEmailsToLowerCase")
    public void convertAffiliationEmailsToLowerCase(MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("email").regex("^.*[A-Z].*$"));
        List<Assertion> assertionsWithUpperCaseEmailCharacters = mongoTemplate.find(query, Assertion.class, "assertion");
        LOG.info("Found {} assertions with upper case emails", assertionsWithUpperCaseEmailCharacters.size());
        assertionsWithUpperCaseEmailCharacters.forEach(a -> {
            LOG.info("Converting assertion with email {} to lower case", a.getEmail());
            a.setEmail(a.getEmail());
            mongoTemplate.save(a);
        });
    }
    
    @ChangeSet(order = "03", author = "George Nash", id = "03-convertOrcidRecordEmailsToLowerCase")
    public void convertOrcidRecordEmailsToLowerCase(MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("email").regex("^.*[A-Z].*$"));
        List<OrcidRecord> orcidRecordWithUpperCaseEmailCharacters = mongoTemplate.find(query, OrcidRecord.class, "orcid_record");
        LOG.info("Found {} orcid records with upper case emails", orcidRecordWithUpperCaseEmailCharacters.size());
        orcidRecordWithUpperCaseEmailCharacters.forEach(or -> {
            LOG.info("Converting orcid record with email {} to lower case", or.getEmail());
            or.setEmail(or.getEmail());
            mongoTemplate.save(or);
        });
    }
    
    @ChangeSet(order = "04", author = "George Nash", id = "04-addProcessedDateToCsvStatsFiles")
    public void addProcessedDateToCsvStatsFiles(MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("file_type").is("assertion-stats"));
        query.addCriteria(Criteria.where("date_processed").is(null));
        List<StoredFile> storedFilesWithNullProcessedDate = mongoTemplate.find(query, StoredFile.class, "stored_file");
        LOG.info("Found {} assertion-stats StoredFiles with null processed date", storedFilesWithNullProcessedDate.size());
        storedFilesWithNullProcessedDate.forEach(f -> {
            LOG.info("Setting processed date to date written value for stored file {}", f.getId());
            f.setDateProcessed(f.getDateWritten());
            mongoTemplate.save(f);
        });
    }

}
