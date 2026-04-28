package org.orcid.mp.assertion.data.migration;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "remove-all-revoked-dates", order = "001", author = "George Nash")
public class RevokedDateCorrection {

    Logger LOG = LoggerFactory.getLogger(RevokedDateCorrection.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        LOG.info("Removing all revoked_dates from orcid_record.tokens field...");
        Query query = new Query(Criteria.where("tokens.revoked_date").exists(true));
        Update update = new Update().unset("tokens.$[].revoked_date");
        long modifiedCount = mongoTemplate.updateMulti(query, update, "orcid_record").getModifiedCount();
        LOG.info("Successfully removed revoked_date from " + modifiedCount + " documents.");
    }

    @RollbackExecution
    public void rollbackExecution(MongoTemplate mongoTemplate) {
        // no roll back
    }
}
