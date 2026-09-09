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
import org.springframework.data.mongodb.core.index.IndexOperations;

@ChangeUnit(id = "add-assertion-email-index", order = "010", author = "George Nash")
public class AddAssertionEmailIndex {

    private static final Logger LOG = LoggerFactory.getLogger(AddAssertionEmailIndex.class);
    private static final String NEW_INDEX_NAME = "email_idx";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Assertion.class);

        Index newIndex = new Index()
                .on("email", Sort.Direction.ASC)
                .named(NEW_INDEX_NAME);

        indexOps.createIndex(newIndex);
        LOG.info("Successfully created optimized index: {}", NEW_INDEX_NAME);
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Assertion.class);
        try {
            indexOps.dropIndex(NEW_INDEX_NAME);
        } catch (Exception e) {
            LOG.warn("Could not drop index during rollback: {}", e.getMessage());
        }
    }
}