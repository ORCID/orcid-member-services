package org.orcid.memberportal.service.user.repository.impl;

import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.repository.CustomUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {


    @Autowired
    private MongoTemplate mongoTemplate;

    public CustomUserRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean updateMemberNames(String salesforceId, String newMemberName) {
        mongoTemplate.updateMulti(Query.query(Criteria.where("salesforceId").is(salesforceId)), new Update().set("memberName", newMemberName), User.class);
        return true;
    }
}
