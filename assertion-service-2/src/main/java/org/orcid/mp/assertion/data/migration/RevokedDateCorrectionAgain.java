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


/**
 * Identical to RevokedDateCorrection but a separate task to record the fact we are carrying out these changes again.
 * This was due to 401s mistakenly being returned by the registry due to performance issues with the auth server.
 */
@ChangeUnit(id = "remove-all-revoked-dates-again", order = "004", author = "George Nash")
public class RevokedDateCorrectionAgain {

    private static final Logger LOG = LoggerFactory.getLogger(RevokedDateCorrectionAgain.class);

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
