package org.orcid.mp.user.data.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChangeUnit(id = "drop-salesforce-id-from-user", order = "001", author = "George Nash") // Adjust order as needed
public class DropSalesforceIdFromUser {

    private static final Logger LOG = LoggerFactory.getLogger(DropSalesforceIdFromUser.class);

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        LOG.info("Starting migration to drop salesforce_id field from jhi_user collection...");

        mongoDatabase.getCollection("jhi_user").updateMany(
                Filters.exists("salesforce_id", true),
                Updates.unset("salesforce_id")
        );

        LOG.info("Successfully dropped salesforce_id from jhi_user collection");
    }

    @RollbackExecution
    public void rollbackExecution(MongoDatabase mongoDatabase) {
        // Data deletion cannot be cleanly rolled back without a backup snapshot
        LOG.warn("Rollback executed for drop-salesforce-id-from-user, but field deletion is irreversible. The field will remain unset.");
    }
}