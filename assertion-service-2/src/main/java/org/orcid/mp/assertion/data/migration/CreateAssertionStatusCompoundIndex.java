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
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.util.List;

@ChangeUnit(id = "create-assertion-status-created-index", order = "005", author = "George Nash")
public class CreateAssertionStatusCompoundIndex {

    private static final Logger LOG = LoggerFactory.getLogger(CreateAssertionStatusCompoundIndex.class);
    private static final String NEW_INDEX_NAME = "status_1_created_1";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Assertion.class);
        List<IndexInfo> existingIndexes = indexOps.getIndexInfo();

        boolean indexExists = existingIndexes.stream()
                .anyMatch(info -> NEW_INDEX_NAME.equals(info.getName()));

        if (!indexExists) {
            Index compoundIndex = new Index()
                    .on("status", Sort.Direction.ASC)
                    .on("created", Sort.Direction.ASC)
                    .named(NEW_INDEX_NAME);

            indexOps.createIndex(compoundIndex);
            LOG.info("Successfully created compound index: {}", NEW_INDEX_NAME);
        } else {
            LOG.info("Compound index {} already exists. Skipping creation.", NEW_INDEX_NAME);
        }
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Assertion.class);
        indexOps.dropIndex(NEW_INDEX_NAME);
        LOG.info("Successfully rolled back and dropped index: {}", NEW_INDEX_NAME);
    }

}
