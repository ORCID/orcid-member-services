package org.orcid.mp.assertion.repository.impl;

import org.orcid.mp.assertion.domain.OrcidRecord;
import org.orcid.mp.assertion.repository.OrcidRecordRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

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
            new Update().set("tokens.$.salesforce_id", newSalesforceId), OrcidRecord.class);
    }
}
