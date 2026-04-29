package org.orcid.mp.assertion.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.orcid.mp.assertion.domain.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;

@ChangeUnit(id = "drop-added-to-orcid-index", order = "002", author = "George Nash")
public class DropAddedToOrcidIndex {

    private static final String INDEX_NAME = "added_to_orcid";

    private static final Logger LOG = LoggerFactory.getLogger(DropAddedToOrcidIndex.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Assertion.class);

        try {
            indexOps.dropIndex(INDEX_NAME);
            LOG.info("Successfully dropped index: {}", INDEX_NAME);
        } catch (Exception e) {
            LOG.warn("Index {}} could not be dropped or does not exist" + INDEX_NAME, e);
        }
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        // don't bother with rollback
    }
}