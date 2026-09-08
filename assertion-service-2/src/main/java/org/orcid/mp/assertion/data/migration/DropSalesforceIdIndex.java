package org.orcid.mp.assertion.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.orcid.mp.assertion.domain.Assertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;

@ChangeUnit(id = "drop-salesforce-id-index", order = "011", author = "George Nash")
public class DropSalesforceIdIndex {

    private static final Logger LOG = LoggerFactory.getLogger(DropSalesforceIdIndex.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        IndexOperations indexOps = mongoTemplate.indexOps(Assertion.class);

        try {
            indexOps.dropIndex("salesforce_id");
            LOG.info("Successfully dropped 'salesforce_id' index.");
        } catch (Exception e) {
            try {
                indexOps.dropIndex("salesforce_id_1");
                LOG.info("Successfully dropped 'salesforce_id_1' index.");
            } catch (Exception ex) {
                LOG.warn("Could not drop salesforce_id index. It may have already been removed: {}", ex.getMessage());
            }
        }
    }

    @RollbackExecution
    public void rollbackExecution() {
        // No rollback necessary for dropping an obsolete index
    }
}