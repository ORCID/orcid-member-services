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
        ProjectionOperation timeModifiedAfterSync = Aggregation.project("added_to_orcid", "updated_in_orcid", "modified", "created", "email", "member_id", "status").andExpression("modified - added_to_orcid").as("timeModifiedAfterAddingToOrcid").andExpression("modified - updated_in_orcid").as("timeModifiedAfterUpdatingInOrcid");

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

        Criteria notDeletedInOrcid = new Criteria();
        notDeletedInOrcid.andOperator(Criteria.where("status").ne(AssertionStatus.USER_DELETED_FROM_ORCID.name()));

        Criteria needsUpdatingInOrcidAndNotDeprecatedOrDeactivatedAndNotDeleted = new Criteria();
        needsUpdatingInOrcidAndNotDeprecatedOrDeactivatedAndNotDeleted.andOperator(needsUpdatingInOrcid, notDeprecatedOrDeactivated, notDeletedInOrcid);

        MatchOperation matchUpdatedAfterSync = Aggregation.match(needsUpdatingInOrcidAndNotDeprecatedOrDeactivatedAndNotDeleted);

        SortOperation sort = new SortOperation(pageable.getSort());
        SkipOperation skip = new SkipOperation(pageable.getOffset());
        LimitOperation limit = new LimitOperation(pageable.getPageSize());

        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(timeModifiedAfterSync);
        operations.add(matchUpdatedAfterSync);
        operations.addAll(buildOrcidRecordValidationStages()); // <-- Inject common logic
        operations.add(sort);
        operations.add(skip);
        operations.add(limit);

        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<Assertion> results = mongoTemplate.aggregate(aggregation, "assertion", Assertion.class);
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

        SortOperation sort = new SortOperation(pageable.getSort());
        SkipOperation skip = new SkipOperation(pageable.getOffset());
        LimitOperation limit = new LimitOperation(pageable.getPageSize());

        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(initialMatch);
        operations.addAll(buildOrcidRecordValidationStages()); // <-- Inject common logic
        operations.add(sort);
        operations.add(skip);
        operations.add(limit);

        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<Assertion> results = mongoTemplate.aggregate(aggregation, "assertion", Assertion.class);
        return results.getMappedResults();
    }

    private List<AggregationOperation> buildOrcidRecordValidationStages() {
        LookupOperation lookupRecord = Aggregation.lookup("orcid_record", "email", "email", "linked_record");
        UnwindOperation unwindRecord = Aggregation.unwind("linked_record", false);

        // Bypass Spring Data's Criteria builder entirely with a custom pipeline stage
        AggregationOperation matchValidTokenAndOrcid = ctx -> {

            org.bson.Document andConditions = new org.bson.Document("$and", java.util.Arrays.asList(
                    // 1. member_id must perfectly match the root document's member_id
                    new org.bson.Document("$eq", java.util.Arrays.asList("$$token.member_id", "$member_id")),

                    // 2. token_id must exist and not be empty (ifNull defaults missing fields to "")
                    new org.bson.Document("$ne", java.util.Arrays.asList(
                            new org.bson.Document("$ifNull", java.util.Arrays.asList("$$token.token_id", "")), ""
                    )),

                    // 3. revoked_date must be missing/null
                    new org.bson.Document("$eq", java.util.Arrays.asList(
                            new org.bson.Document("$ifNull", java.util.Arrays.asList("$$token.revoked_date", null)), null
                    )),

                    // 4. denied_date must be missing/null
                    new org.bson.Document("$eq", java.util.Arrays.asList(
                            new org.bson.Document("$ifNull", java.util.Arrays.asList("$$token.denied_date", null)), null
                    ))
            ));

            // Safely handle if linked_record.tokens is entirely missing
            org.bson.Document filterInput = new org.bson.Document("$ifNull",
                    java.util.Arrays.asList("$linked_record.tokens", java.util.Collections.emptyList()));

            org.bson.Document filter = new org.bson.Document("$filter", new org.bson.Document("input", filterInput)
                    .append("as", "token")
                    .append("cond", andConditions));

            org.bson.Document size = new org.bson.Document("$size", filter);
            org.bson.Document expr = new org.bson.Document("$expr", new org.bson.Document("$gt", java.util.Arrays.asList(size, 0)));

            // Combine the standard orcid check with the raw $expr array check
            org.bson.Document matchLogic = new org.bson.Document()
                    .append("linked_record.orcid", new org.bson.Document("$exists", true).append("$ne", null).append("$nin", java.util.Arrays.asList("")))
                    .append("$expr", expr.get("$expr"));

            // Return the pure $match stage
            return new org.bson.Document("$match", matchLogic);
        };

        return java.util.Arrays.asList(lookupRecord, unwindRecord, matchValidTokenAndOrcid);
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