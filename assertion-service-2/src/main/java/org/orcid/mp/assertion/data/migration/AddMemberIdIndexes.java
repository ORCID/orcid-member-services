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

@ChangeUnit(id = "addMemberIdUiIndexes", order = "010", author = "George Nash")
public class AddMemberIdIndexes {

    private static final Logger LOG = LoggerFactory.getLogger(AddMemberIdIndexes.class);

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        Index statusIndex = new Index()
                .on("member_id", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .on("_id", Sort.Direction.DESC)
                .named("member_id_1_status_1_id_-1");
        mongoTemplate.indexOps(Assertion.class).createIndex(statusIndex);

        Index emailIndex = new Index()
                .on("member_id", Sort.Direction.ASC)
                .on("email", Sort.Direction.ASC)
                .on("_id", Sort.Direction.ASC)
                .named("member_id_1_email_1_id_1");
        mongoTemplate.indexOps(Assertion.class).createIndex(emailIndex);

        LOG.info("Successfully created member_id indexes.");
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.indexOps(Assertion.class).dropIndex("member_id_1_status_1_id_-1");
        mongoTemplate.indexOps(Assertion.class).dropIndex("member_id_1_email_1_id_1");
    }

}
