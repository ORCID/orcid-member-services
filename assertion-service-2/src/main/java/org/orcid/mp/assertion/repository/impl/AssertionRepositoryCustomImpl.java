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

import java.util.ArrayList;
import java.util.Arrays;
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
        Criteria addedToOrcidSet = new Criteria().andOperator(
                Criteria.where("added_to_orcid").exists(true),
                Criteria.where("added_to_orcid").ne(null)
        );

        Criteria updatedInOrcidSet = new Criteria().andOperator(
                Criteria.where("updated_in_orcid").exists(true),
                Criteria.where("updated_in_orcid").ne(null)
        );

        Criteria updatedInOrcidNotSet = new Criteria().orOperator(
                Criteria.where("updated_in_orcid").exists(false),
                Criteria.where("updated_in_orcid").is(null)
        );

        AggregationExpression modifiedAfterUpdated = ComparisonOperators.valueOf("modified")
                .greaterThan("updated_in_orcid");

        Criteria modifiedAfterUpdateInOrcid = new Criteria().andOperator(
                updatedInOrcidSet,
                Criteria.expr(modifiedAfterUpdated)
        );

        AggregationExpression modifiedAfterAdded = ComparisonOperators.valueOf("modified")
                .greaterThan("added_to_orcid");

        Criteria modifiedAfterAddingToOrcidAndUpdateInOrcidNotSet = new Criteria().andOperator(
                addedToOrcidSet,
                updatedInOrcidNotSet,
                Criteria.expr(modifiedAfterAdded)
        );

        Criteria needsUpdatingInOrcid = new Criteria().orOperator(
                modifiedAfterUpdateInOrcid,
                modifiedAfterAddingToOrcidAndUpdateInOrcidNotSet
        );

        Criteria notDeprecatedOrDeactivated = Criteria.where("status").ne(AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name());
        Criteria notDeletedInOrcid = Criteria.where("status").ne(AssertionStatus.USER_DELETED_FROM_ORCID.name());
        Criteria hasValidToken = Criteria.where("token_available").is(true);

        Criteria finalCriteria = new Criteria().andOperator(
                needsUpdatingInOrcid,
                notDeprecatedOrDeactivated,
                notDeletedInOrcid,
                hasValidToken
        );

        MatchOperation initialMatch = Aggregation.match(finalCriteria);

        SortOperation sort = new SortOperation(pageable.getSort());
        SkipOperation skip = new SkipOperation(pageable.getOffset());
        LimitOperation limit = new LimitOperation(pageable.getPageSize());

        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(initialMatch);
        operations.add(sort);
        operations.add(skip);
        operations.add(limit);

        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<Assertion> results = mongoTemplate.aggregate(aggregation, "assertion", Assertion.class);
        return results.getMappedResults();
    }

    @Override
    public List<Assertion> findAllToCreateInOrcidRegistry(Pageable pageable) {
        Criteria applicableStatus = Criteria.where("status").in(
                AssertionStatus.PENDING.name(),
                AssertionStatus.NOTIFICATION_SENT.name(),
                AssertionStatus.NOTIFICATION_FAILED.name(),
                AssertionStatus.USER_REVOKED_ACCESS.name()
        );

        Criteria notAddedToOrcid = new Criteria();
        notAddedToOrcid.orOperator(Criteria.where("added_to_orcid").exists(false), Criteria.where("added_to_orcid").is(null));

        Criteria hasValidToken = Criteria.where("token_available").is(true);

        Criteria finalCriteria = new Criteria().andOperator(
                applicableStatus,
                notAddedToOrcid,
                hasValidToken
        );

        MatchOperation initialMatch = Aggregation.match(finalCriteria);

        SortOperation sort = new SortOperation(pageable.getSort());
        SkipOperation skip = new SkipOperation(pageable.getOffset());
        LimitOperation limit = new LimitOperation(pageable.getPageSize());

        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(initialMatch);
        operations.add(sort);
        operations.add(skip);
        operations.add(limit);

        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<Assertion> results = mongoTemplate.aggregate(aggregation, "assertion", Assertion.class);
        return results.getMappedResults();
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