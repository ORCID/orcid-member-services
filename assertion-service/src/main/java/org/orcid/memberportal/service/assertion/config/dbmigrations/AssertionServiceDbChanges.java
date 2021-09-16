package org.orcid.memberportal.service.assertion.config.dbmigrations;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;

@ChangeLog(order = "001")
public class AssertionServiceDbChanges {

    @ChangeSet(order = "01", author = "George Nash", id = "01-populateLastSyncAttempts")
    public void addAuthorities(MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("last_sync_attempt").exists(false));
        List<Assertion> assertionsWithEmptyLastSyncAttempt = mongoTemplate.find(query, Assertion.class, "assertion");
        assertionsWithEmptyLastSyncAttempt.forEach(a -> {
            if (a.getUpdatedInORCID() != null) {
                a.setLastSyncAttempt(a.getUpdatedInORCID());
                mongoTemplate.save(a);
            } else if (a.getAddedToORCID() != null) {
                a.setLastSyncAttempt(a.getAddedToORCID());
                mongoTemplate.save(a);
            }
        });
    }

}
