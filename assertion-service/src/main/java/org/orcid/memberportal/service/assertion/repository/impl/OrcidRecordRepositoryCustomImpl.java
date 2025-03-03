package org.orcid.memberportal.service.assertion.repository.impl;

import com.mongodb.MongoClient;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.model.Filters;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.MemberAssertionStatusCount;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.repository.AssertionRepositoryCustom;
import org.orcid.memberportal.service.assertion.repository.OrcidRecordRepositoryCustom;
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
public class OrcidRecordRepositoryCustomImpl implements OrcidRecordRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    public OrcidRecordRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }


    @Override
    public void updateTokenSalesforceIds(String oldSalesforceId, String newSalesforceId) {
        mongoTemplate.updateMulti(Query.query(Criteria.where("tokens.salesforce_id").is(oldSalesforceId)),
            new Update().set("tokens.$.salesforce_id", oldSalesforceId), OrcidRecord.class);
    }
}
