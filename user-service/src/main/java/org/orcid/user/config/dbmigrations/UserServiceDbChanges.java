package org.orcid.user.config.dbmigrations;

import java.time.Instant;

import org.orcid.user.domain.User;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;

@ChangeLog(order = "002")
public class UserServiceDbChanges {
	
    @ChangeSet(order = "01", author = "George Nash", id = "01-changeAdminEmail")
    public void addAuthorities(MongoTemplate mongoTemplate) {
    	Query query = new Query();
    	query.addCriteria(Criteria.where("email").is("admin@orcid.org"));
    	User adminUser = mongoTemplate.findOne(query, User.class, "jhi_user");
    	adminUser.setEmail("member-portal@orcid.org");
    	adminUser.setLastModifiedDate(Instant.now());
    	adminUser.setLastModifiedBy("system");
    	mongoTemplate.save(adminUser);
    }

}
