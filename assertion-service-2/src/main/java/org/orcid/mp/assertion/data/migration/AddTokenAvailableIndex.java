package org.orcid.mp.assertion.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.orcid.mp.assertion.domain.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@ChangeUnit(id = "addTokenAvailableIndexAndDropOld", order = "006", author = "system")
public class AddTokenAvailableIndex {

    private static final Logger LOG = LoggerFactory.getLogger(AddTokenAvailableIndex.class);
    private static final String NEW_INDEX_NAME = "status_1_token_available_1_created_1";
    private static final String OLD_INDEX_NAME = "status_1_created_1";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        // 1. Drop the old obsolete index
        try {
            mongoTemplate.indexOps(Assertion.class).dropIndex(OLD_INDEX_NAME);
            LOG.info("Successfully dropped old index: {}", OLD_INDEX_NAME);
        } catch (Exception e) {
            LOG.warn("Could not drop old index {} (it may not exist)", OLD_INDEX_NAME);
        }

        // 2. Create the new ESR Index: Equality (status, token_available), Sort (created ASC)
        Index esrIndex = new Index()
                .on("status", Sort.Direction.ASC)
                .on("token_available", Sort.Direction.ASC)
                .on("created", Sort.Direction.ASC)
                .named(NEW_INDEX_NAME);

        mongoTemplate.indexOps(Assertion.class).createIndex(esrIndex);
        LOG.info("Successfully created new index: {}", NEW_INDEX_NAME);
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        // Drop the new index if we need to roll back
        mongoTemplate.indexOps(Assertion.class).dropIndex(NEW_INDEX_NAME);

        // Recreate the old index just in case (matching OLD_INDEX_NAME: status_1_created_1)
        Index oldIndex = new Index()
                .on("status", Sort.Direction.ASC)
                .on("created", Sort.Direction.ASC)
                .named(OLD_INDEX_NAME);
        mongoTemplate.indexOps(Assertion.class).createIndex(oldIndex);
    }
}