package org.orcid.mp.assertion.repository.impl;

import com.mongodb.client.DistinctIterable;
import com.mongodb.client.model.Filters;
import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.domain.AssertionStatus;
import org.orcid.mp.assertion.domain.MemberAssertionStatusCount;
import org.orcid.mp.assertion.repository.AssertionRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Iterator;
import java.util.List;

@Repository
public class AssertionRepositoryCustomImpl implements AssertionRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public AssertionRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Assertion> findAllToUpdateInOrcidRegistry(Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").in(AssertionStatus.PENDING_RETRY.name(), AssertionStatus.PENDING_UPDATE.name()));
        query.addCriteria(Criteria.where("token_available").is(true));
        query.with(pageable);
        return mongoTemplate.find(query, Assertion.class);
    }

    @Override
    public List<Assertion> findAllToCreateInOrcidRegistry(Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").in(
                AssertionStatus.PENDING.name(),
                AssertionStatus.PENDING_RETRY.name(),
                AssertionStatus.NOTIFICATION_SENT.name(),
                AssertionStatus.NOTIFICATION_FAILED.name(),
                AssertionStatus.USER_REVOKED_ACCESS.name()
        ));
        query.addCriteria(new Criteria().orOperator(
                Criteria.where("added_to_orcid").exists(false),
                Criteria.where("added_to_orcid").is(null)
        ));
        query.addCriteria(Criteria.where("token_available").is(true));
        query.with(pageable);
        return mongoTemplate.find(query, Assertion.class, "assertion");
    }

    @Override
    public List<MemberAssertionStatusCount> getMemberAssertionStatusCounts() {
        GroupOperation countByStatus = Aggregation.group("member_id", "status").count().as("statusCount");
        ProjectionOperation projection = Aggregation.project().andExpression("_id.member_id").as("memberId").andExpression("status").as("status").andExpression("statusCount").as("statusCount");
        Aggregation aggregation = Aggregation.newAggregation(countByStatus, projection);
        AggregationResults<MemberAssertionStatusCount> results = mongoTemplate.aggregate(aggregation, "assertion", MemberAssertionStatusCount.class);
        return results.getMappedResults();
    }

    @Override
    public void updateStatusPendingOrNotificationFailedToNotificationRequested(String memberId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("member_id").is(memberId).and("status").in(AssertionStatus.PENDING.name(), AssertionStatus.NOTIFICATION_FAILED.name()));
        Update update = new Update();
        update.set("status", AssertionStatus.NOTIFICATION_REQUESTED.name());
        mongoTemplate.updateMulti(query, update, Assertion.class, "assertion");
    }

    @Override
    public Iterator<String> findDistinctEmailsWithNotificationRequested(String memberId) {
        DistinctIterable<String> distinctIterable = mongoTemplate.getCollection("assertion").distinct("email", Filters.and(Filters.eq("status", AssertionStatus.NOTIFICATION_REQUESTED.name()), Filters.eq("member_id", memberId)), String.class);
        return distinctIterable.iterator();
    }
}