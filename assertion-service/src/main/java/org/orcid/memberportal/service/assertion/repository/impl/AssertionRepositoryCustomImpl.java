package org.orcid.memberportal.service.assertion.repository.impl;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.MemberAssertionStatusCount;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.repository.AssertionRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class AssertionRepositoryCustomImpl implements AssertionRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public AssertionRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<Assertion> findAllToUpdateInOrcidRegistry(Pageable pageable) {
        ProjectionOperation timeModifiedAfterSync = Aggregation.project("added_to_orcid", "updated_in_orcid", "modified", "created")
                .andExpression("modified - added_to_orcid").as("timeModifiedAfterAddingToOrcid").andExpression("modified - updated_in_orcid")
                .as("timeModifiedAfterUpdatingInOrcid");

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

        MatchOperation matchUpdatedAfterSync = Aggregation.match(needsUpdatingInOrcid);

        // pagination aggregation operations
        SortOperation sort = new SortOperation(pageable.getSort());
        SkipOperation skip = new SkipOperation(pageable.getOffset());
        LimitOperation limit = new LimitOperation(pageable.getPageSize());

        Aggregation aggregation = Aggregation.newAggregation(timeModifiedAfterSync, matchUpdatedAfterSync, sort, skip, limit);
        AggregationResults<Assertion> results = mongoTemplate.aggregate(aggregation, "assertion", Assertion.class);

        return results.getMappedResults();
    }

    @Override
    public List<MemberAssertionStatusCount> getMemberAssertionStatusCounts() {
        GroupOperation countByStatus = Aggregation.group("salesforce_id", "status").count().as("statusCount");
        ProjectionOperation projection = Aggregation.project().andExpression("_id.salesforce_id").as("salesforceId").andExpression("status").as("status")
                .andExpression("statusCount").as("statusCount");
        Aggregation aggregation = Aggregation.newAggregation(countByStatus, projection);
        AggregationResults<MemberAssertionStatusCount> results = mongoTemplate.aggregate(aggregation, "assertion", MemberAssertionStatusCount.class);
        return results.getMappedResults();
    }

    @Override
    public List<Assertion> findAllToCreateInOrcidRegistry(Pageable pageable) {
        Criteria criteria = new Criteria();
        criteria.orOperator(Criteria.where("added_to_orcid").exists(false), Criteria.where("added_to_orcid").is(null));
        Query query = new Query(criteria);
        query.with(pageable);
        return mongoTemplate.find(query, Assertion.class);
    }

    @Override
    public void updateStatusPendingToNotificationRequested(String salesforceId) {
        Query query = new Query();
        query.addCriteria(Criteria
                        .where("salesforceId").is(salesforceId).and("status").is(AssertionStatus.PENDING.name()));
        Update update = new Update();
        update.set("status", AssertionStatus.NOTIFICATION_REQUESTED.name());
        mongoTemplate.updateMulti(query, update, Assertion.class, "assertion");
    }
    
    @Override
    public List<Assertion> findEmailAndSalesforceIdsWithNotificationRequested() {
        ProjectionOperation project = Aggregation.project("email", "salesforce_id", "status");
        MatchOperation match = Aggregation.match(Criteria.where("status").is(AssertionStatus.NOTIFICATION_REQUESTED.name()));
        GroupOperation group = Aggregation.group("email", "salesforce_id");
        Aggregation aggregation = Aggregation.newAggregation(project, match, group);
        AggregationResults<Assertion> results = mongoTemplate.aggregate(aggregation, "assertion", Assertion.class);
        return results.getMappedResults();
    }

}
