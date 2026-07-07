package org.orcid.mp.assertion.data.migration;

import com.mongodb.client.MongoDatabase;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChangeUnit(id = "drop-redundant-added-to-orcid-index", order = "006", author = "George Nash")
public class DropRedundantCompoundIndex {

    private static final Logger LOG = LoggerFactory.getLogger(DropRedundantCompoundIndex.class);

    @Execution
    public void execution(MongoDatabase mongoDatabase) {
        mongoDatabase.getCollection("assertion").dropIndex("added_to_orcid_1_created_1_status_1");
        LOG.info("Successfully dropped redundant compound index");
    }

    @RollbackExecution
    public void rollbackExecution(MongoDatabase mongoDatabase) {
        Document keys = new Document("added_to_orcid", 1)
                .append("created", 1)
                .append("status", 1);

        mongoDatabase.getCollection("assertion").createIndex(keys);
    }
}