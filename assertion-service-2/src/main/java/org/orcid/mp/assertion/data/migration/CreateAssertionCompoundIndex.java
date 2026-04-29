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

@ChangeUnit(id = "create-assertion-compound-index", order = "003", author = "George Nash")
public class CreateAssertionCompoundIndex {

    private static final String INDEX_NAME = "added_to_orcid_1_created_1_status_1";

    private static final Logger LOG = LoggerFactory.getLogger(CreateAssertionCompoundIndex.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Assertion.class);

        List<IndexInfo> existingIndexes = indexOps.getIndexInfo();

        boolean indexExists = existingIndexes.stream()
                .anyMatch(info -> INDEX_NAME.equals(info.getName()));

        if (!indexExists) {
            Index compoundIndex = new Index()
                    .on("added_to_orcid", Sort.Direction.ASC)
                    .on("created", Sort.Direction.ASC)
                    .on("status", Sort.Direction.ASC)
                    .named(INDEX_NAME);

            indexOps.createIndex(compoundIndex);
            LOG.info("Successfully created compound index: {}", INDEX_NAME);
        } else {
            LOG.info("Compound index {} }already exists. Skipping creation.", INDEX_NAME);
        }
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
    }
}
