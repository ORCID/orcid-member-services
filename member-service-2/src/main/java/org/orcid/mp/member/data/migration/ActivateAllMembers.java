package org.orcid.mp.member.data.migration;

import com.mongodb.client.result.UpdateResult;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "activateAllMembers", order = "001", author = "George Nash")
public class ActivateAllMembers {

    private static final Logger LOG = LoggerFactory.getLogger(ActivateAllMembers.class);
    private static final String COLLECTION_NAME = "member";

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        LOG.info("Starting migration to set active=true for all members...");

        Query allDocsQuery = new Query();
        Update setActiveUpdate = new Update().set("active", true);

        UpdateResult result = mongoTemplate.updateMulti(allDocsQuery, setActiveUpdate, COLLECTION_NAME);

        LOG.info("Successfully updated {} member documents to active=true.", result.getMatchedCount());
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        LOG.warn("Rolling back member activation. Setting active=false for all members.");

        Query allDocsQuery = new Query();
        Update setInactiveUpdate = new Update().set("active", false);

        mongoTemplate.updateMulti(allDocsQuery, setInactiveUpdate, COLLECTION_NAME);
    }
}