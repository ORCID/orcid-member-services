package org.orcid.mp.member.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "drop-salesforce-updated-field", order = "002", author = "George Nash")
public class DropLastUpdatedBySalesforce {

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        // An empty query matches all documents in the collection
        Query query = new Query();

        // The $unset operator removes the specified key completely
        Update update = new Update().unset("last_updated_with_salesforce_data");

        // Apply the update to the "member" collection
        mongoTemplate.updateMulti(query, update, "member");
    }

    @RollbackExecution
    public void rollbackExecution() {
        // Dropping a field destroys data, which cannot be magically recovered in a rollback.
        // This is intentionally left blank. If a rollback is triggered, the field will
        // simply remain absent, which matches the desired end state anyway.
    }
}