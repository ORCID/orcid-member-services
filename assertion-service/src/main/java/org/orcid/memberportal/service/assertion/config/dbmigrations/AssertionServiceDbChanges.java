package org.orcid.memberportal.service.assertion.config.dbmigrations;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
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

    @ChangeSet(order = "05", author = "George Nash", id = "05-makeOrcidRecordEmailIndexUnique")
    public void makeOrcidRecordEmailIndexUnique(MongoTemplate mongoTemplate) {
        try {
            mongoTemplate.indexOps("orcid_record").dropIndex("email");
        } catch (Exception e) {
            // do nothing - IT tests will not find index called email as it was
            // previously generated from annotation in the domain object
        }
        mongoTemplate.indexOps("orcid_record").ensureIndex(new Index("email", Direction.ASC).unique().named("email_unique_idx"));
    }

    @ChangeSet(order = "06", author = "George Nash", id = "06-removeOrcidIdsFromAssertionsWithNoTokenAssociated")
    public void removeOrcidIdsFromAssertionsWithNoTokenAssociated(MongoTemplate mongoTemplate) {
        Query assertionsQuery = new Query();
        assertionsQuery.addCriteria(Criteria.where("orcid_id").exists(true));
        List<Assertion> assertionsWithOrcidId = mongoTemplate.find(assertionsQuery, Assertion.class, "assertion");
        
        LOG.info("Found {} assertions with orcid id populated", assertionsWithOrcidId.size());
        int numRemoved = 0;
        
        for (Assertion a : assertionsWithOrcidId) {
            Query orcidRecordQuery = new Query();
            orcidRecordQuery.addCriteria(Criteria.where("email").is(a.getEmail()));
            OrcidRecord orcidRecord = mongoTemplate.findOne(orcidRecordQuery, OrcidRecord.class, "orcid_record");
            if (orcidRecord.getTokens() == null || orcidRecord.getTokens().isEmpty() || orcidRecord.getToken(a.getSalesforceId(), false) == null) {
                // either no token or token is revoked / denied etc, remove orcid id from assertion
                LOG.info("Removing orcid id from affiliation {}", a.getId());
                a.setOrcidId(null);
                mongoTemplate.save(a);
                numRemoved++;
            }
        }
        
        LOG.info("{} orcid ids removed from affiliations", numRemoved);
    }

}
