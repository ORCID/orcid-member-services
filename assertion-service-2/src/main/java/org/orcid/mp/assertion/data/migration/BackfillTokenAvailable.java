package org.orcid.mp.assertion.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.types.ObjectId;
import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.domain.OrcidRecord;
import org.orcid.mp.assertion.domain.OrcidToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ChangeUnit(id = "backfillTokenAvailableData", order = "007", author = "system")
public class BackfillTokenAvailable {

    private static final Logger LOG = LoggerFactory.getLogger(BackfillTokenAvailable.class);
    private static final int BATCH_SIZE = 1000;

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        LOG.info("Starting Java-based batch backfill of token_available flag...");

        ObjectId lastId = null;
        int totalProcessed = 0;

        while (true) {
            // 1. Fetch a batch of Assertions (using fast _id Keyset Pagination)
            Query batchQuery = new Query().with(Sort.by(Sort.Direction.ASC, "_id")).limit(BATCH_SIZE);
            if (lastId != null) {
                batchQuery.addCriteria(Criteria.where("_id").gt(lastId));
            }

            List<Assertion> assertions = mongoTemplate.find(batchQuery, Assertion.class);
            if (assertions.isEmpty()) {
                break; // collection processed
            }

            Set<String> emails = assertions.stream()
                    .map(Assertion::getEmail)
                    .collect(Collectors.toSet());

            Query orcidQuery = new Query(Criteria.where("email").in(emails));
            List<OrcidRecord> orcidRecords = mongoTemplate.find(orcidQuery, OrcidRecord.class, "orcid_record");

            Map<String, OrcidRecord> recordsByEmail = orcidRecords.stream()
                    .collect(Collectors.toMap(
                            rec -> rec.getEmail(),
                            rec -> rec,
                            (existing, replacement) -> existing // Keep existing if duplicates exist
                    ));

            // 3. Prepare a Bulk Update operation
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Assertion.class);

            // 4. Run your readable Java logic
            for (Assertion assertion : assertions) {
                OrcidRecord record = recordsByEmail.get(assertion.getEmail());
                boolean isValid = hasValidToken(assertion, record);

                Query updateQuery = new Query(Criteria.where("_id").is(assertion.getId()));
                Update update = new Update().set("token_available", isValid);

                bulkOps.updateOne(updateQuery, update);
                lastId = new ObjectId(assertion.getId()); // Track for next batch
            }

            // 5. Execute the bulk update for these 1,000 records
            bulkOps.execute();

            totalProcessed += assertions.size();
            LOG.info("Processed {} assertions...", totalProcessed);
        }
        LOG.info("Finished backfilling token_available for {} total assertions.", totalProcessed);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        LOG.warn("Rolling back token_available backfill. Stripping field from all assertions.");
        Query allDocsQuery = new Query();
        Update unsetUpdate = new Update().unset("token_available");
        mongoTemplate.updateMulti(allDocsQuery, unsetUpdate, Assertion.class);
    }

    private boolean hasValidToken(Assertion assertion, OrcidRecord orcidRecord) {
        if (orcidRecord == null) {
            return false;
        }
        if (orcidRecord.getTokens() == null) {
            return false;
        }

        for (OrcidToken token : orcidRecord.getTokens()) {
            if (Objects.equals(token.getMemberId(), assertion.getMemberId()) &&
                    token.getTokenId() != null) {
                return true;
            }
        }
        return false;
    }
}