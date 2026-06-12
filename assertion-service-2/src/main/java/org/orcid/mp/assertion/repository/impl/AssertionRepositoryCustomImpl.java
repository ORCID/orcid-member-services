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
        ProjectionOperation timeModifiedAfterSync = Aggregation.project("added_to_orcid", "updated_in_orcid", "modified", "created", "email", "memberId", "status")
                .andExpression("modified - added_to_orcid").as("timeModifiedAfterAddingToOrcid")
                .andExpression("modified - updated_in_orcid").as("timeModifiedAfterUpdatingInOrcid");

        Criteria addedToOrcidSet = new Criteria();
        addedToOrcidSet.andOperator(Criteria.where("added_to_orcid").exists(true), Criteria.where("added_to_orcid").ne(null));

        Criteria updatedInOrcidSet = new Criteria();
        updatedInOrcidSet.andOperator(Criteria.where("updated_in_orcid").exists(true), Criteria.where("updated_in_orcid").ne(null));

        Criteria updatedInOrcidNotSet = new Criteria();
        updatedInOrcidNotSet.orOperator(Criteria.where("updated_in_orcid").exists(false), Criteria.where("updated_in_orcid").is(null));

        Criteria modifiedAfterUpdateInOrcid = new Criteria();
        modifiedAfterUpdateInOrcid.andOperator(updatedInOrcidSet, Criteria.where("timeModifiedAfterUpdatingInOrcid").gt(0));

        Criteria modifiedAfterAddingToOrcidAndUpdateInOrcidNotSet = new Criteria();
        modifiedAfterAddingToOrcidAndUpdateInOrcidNotSet.andOperator(addedToOrcidSet, updatedInOrcidNotSet, Criteria.where("timeModifiedAfterAddingToOrcid").gt(0));

        Criteria needsUpdatingInOrcid = new Criteria();
        needsUpdatingInOrcid.orOperator(modifiedAfterUpdateInOrcid, modifiedAfterAddingToOrcidAndUpdateInOrcidNotSet);

        Criteria notDeprecatedOrDeactivated = new Criteria();
        notDeprecatedOrDeactivated.andOperator(Criteria.where("status").ne(AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name()));

        Criteria needsUpdatingInOrcidAndNotDeprecatedOrDeactivated = new Criteria();
        needsUpdatingInOrcidAndNotDeprecatedOrDeactivated.andOperator(needsUpdatingInOrcid, notDeprecatedOrDeactivated);

        MatchOperation matchUpdatedAfterSync = Aggregation.match(needsUpdatingInOrcidAndNotDeprecatedOrDeactivated);

        // Lookup operation to join the 'orcid_record' collection using the 'email' field
        LookupOperation lookupRecord = Aggregation.lookup("orcid_record", "email", "email", "linked_record");

        // Added Unwind operation to flatten the joined array.
        // preserveNullAndEmptyArrays = false acts as an INNER JOIN, dropping assertions without an orcid_record entirely.
        UnwindOperation unwindRecord = Aggregation.unwind("linked_record", false);

        // Built a pure Java AggregationExpression for the individual token matching logic
        AggregationExpression tokenCondition = BooleanOperators.And.and(
                ComparisonOperators.Eq.valueOf("$$token.memberId").equalTo("$memberId"),
                ComparisonOperators.Ne.valueOf("$$token.tokenId").notEqualToValue(null),
                ComparisonOperators.Ne.valueOf("$$token.tokenId").notEqualToValue(""),
                ComparisonOperators.Eq.valueOf("$$token.revokedDate").equalToValue(null),
                ComparisonOperators.Eq.valueOf("$$token.deniedDate").equalToValue(null)
        );

        // token condition to filter the 'linked_record.tokens' array
        AggregationExpression validTokensFilter = ArrayOperators.Filter.filter("linked_record.tokens")
                .as("token")
                .by(tokenCondition);

        AggregationExpression hasValidTokens = ComparisonOperators.Gt.valueOf(ArrayOperators.Size.lengthOfArray(validTokensFilter)).greaterThanValue(0);

        // check ORCID id in orcid_record
        Criteria validRecordCriteria = new Criteria().andOperator(
                Criteria.where("linked_record.orcid").exists(true).ne(null).ne(""),
                Criteria.expr(hasValidTokens)
        );

        MatchOperation matchValidTokenAndOrcid = Aggregation.match(validRecordCriteria);

        // pagination aggregation operations
        SortOperation sort = new SortOperation(pageable.getSort());
        SkipOperation skip = new SkipOperation(pageable.getOffset());
        LimitOperation limit = new LimitOperation(pageable.getPageSize());

        Aggregation aggregation = Aggregation.newAggregation(
                timeModifiedAfterSync,
                matchUpdatedAfterSync,
                lookupRecord,
                unwindRecord,
                matchValidTokenAndOrcid,
                sort,
                skip,
                limit
        );

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
    public List<Assertion> findAllToCreateInOrcidRegistry(Pageable pageable) {
        Criteria notAddedToOrcid = new Criteria();
        notAddedToOrcid.orOperator(Criteria.where("added_to_orcid").exists(false), Criteria.where("added_to_orcid").is(null));

        Criteria notDeprecatedOrDeactivated = new Criteria();
        notDeprecatedOrDeactivated.andOperator(Criteria.where("status").ne(AssertionStatus.RECORD_DEACTIVATED_OR_DEPRECATED.name()));

        Criteria notAddedToOrcidAndNotDeprecatedOrDeactivated = new Criteria();
        notAddedToOrcidAndNotDeprecatedOrDeactivated.andOperator(notAddedToOrcid, notDeprecatedOrDeactivated);

        MatchOperation initialMatch = Aggregation.match(notAddedToOrcidAndNotDeprecatedOrDeactivated);

        // add lookup operation to join the 'orcid_record' collection using the 'email' field
        LookupOperation lookupRecord = Aggregation.lookup("orcid_record", "email", "email", "linked_record");
        UnwindOperation unwindRecord = Aggregation.unwind("linked_record", false);

        // token matching logic
        AggregationExpression tokenCondition = BooleanOperators.And.and(
                ComparisonOperators.Eq.valueOf("$$token.memberId").equalTo("$memberId"),
                ComparisonOperators.Ne.valueOf("$$token.tokenId").notEqualToValue(null),
                ComparisonOperators.Ne.valueOf("$$token.tokenId").notEqualToValue(""),
                ComparisonOperators.Eq.valueOf("$$token.revokedDate").equalToValue(null),
                ComparisonOperators.Eq.valueOf("$$token.deniedDate").equalToValue(null)
        );

        // apply token condition to filter the 'linked_record.tokens' array
        AggregationExpression validTokensFilter = ArrayOperators.Filter.filter("linked_record.tokens")
                .as("token")
                .by(tokenCondition);

        AggregationExpression hasValidTokens = ComparisonOperators.Gt.valueOf(ArrayOperators.Size.lengthOfArray(validTokensFilter)).greaterThanValue(0);

        Criteria validRecordCriteria = new Criteria().andOperator(
                Criteria.where("linked_record.orcid").exists(true).ne(null).ne(""),
                Criteria.expr(hasValidTokens)
        );

        MatchOperation matchValidTokenAndOrcid = Aggregation.match(validRecordCriteria);

        SortOperation sort = new SortOperation(pageable.getSort());
        SkipOperation skip = new SkipOperation(pageable.getOffset());
        LimitOperation limit = new LimitOperation(pageable.getPageSize());

        Aggregation aggregation = Aggregation.newAggregation(
                initialMatch,
                lookupRecord,
                unwindRecord,
                matchValidTokenAndOrcid,
                sort,
                skip,
                limit
        );

        AggregationResults<Assertion> results = mongoTemplate.aggregate(aggregation, "assertion", Assertion.class);
        return results.getMappedResults();
    }

    @Override
    public void updateStatusPendingOrNotificationFailedToNotificationRequested(String memberId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("memberId").is(memberId).and("status").in(AssertionStatus.PENDING.name(), AssertionStatus.NOTIFICATION_FAILED.name()));
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
