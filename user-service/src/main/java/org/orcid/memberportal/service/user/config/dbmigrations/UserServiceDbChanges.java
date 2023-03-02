package org.orcid.memberportal.service.user.config.dbmigrations;

import java.time.Instant;
import java.util.List;

import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.security.AuthoritiesConstants;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

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
    
    @ChangeSet(order = "02", author = "George Nash", id = "02-removeLoginField")
    public void removeLoginField(MongoTemplate mongoTemplate) {
        Update update = new Update();
        update.unset("login");
        mongoTemplate.updateMulti(new Query(), update, "jhi_user");
    }
    
    @ChangeSet(order = "03", author = "George Nash", id = "03-updateAdminFlag")
    public void updateAdminFlag(MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("authorities").is(AuthoritiesConstants.ADMIN));
        List<User> adminUsers = mongoTemplate.find(query, User.class);
        adminUsers.forEach((u -> {
            u.setAdmin(true);
            mongoTemplate.save(u);
        }));
    }

}
