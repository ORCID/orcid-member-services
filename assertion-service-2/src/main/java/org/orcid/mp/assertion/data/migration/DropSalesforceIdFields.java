package org.orcid.mp.assertion.data.migration;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChangeUnit(id = "drop-salesforce-id-fields", order = "009", author = "George Nash")
public class DropSalesforceIdFields {

    private static final Logger LOG = LoggerFactory.getLogger(DropSalesforceIdFields.class);

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        LOG.info("Starting migration to drop salesforce_id fields...");

        mongoDatabase.getCollection("assertion").updateMany(
                Filters.exists("salesforce_id", true),
                Updates.unset("salesforce_id")
        );
        LOG.info("Dropped salesforce_id from assertion collection");

        mongoDatabase.getCollection("send_notifications_request").updateMany(
                Filters.exists("salesforce_id", true),
                Updates.unset("salesforce_id")
        );
        LOG.info("Dropped salesforce_id from send_notifications_request collection");

        mongoDatabase.getCollection("orcid_record").updateMany(
                Filters.exists("tokens.salesforce_id", true),
                Updates.unset("tokens.$[].salesforce_id")
        );
        LOG.info("Dropped salesforce_id from tokens array in orcid_record collection");
    }

    @RollbackExecution
    public void rollbackExecution(MongoDatabase mongoDatabase) {
        LOG.warn("Rollback executed for drop-salesforce-id-fields, but field deletion is irreversible. Fields will remain unset.");
    }
}
