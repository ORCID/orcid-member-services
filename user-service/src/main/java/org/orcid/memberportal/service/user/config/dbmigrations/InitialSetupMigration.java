package org.orcid.memberportal.service.user.config.dbmigrations;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.orcid.memberportal.service.user.domain.User;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;

/**
 * Creates the initial database setup.
 */
@ChangeLog(order = "001")
public class InitialSetupMigration {

    // note change set 2 because 1 involved adding authorities to do the db, which are no longer used
    @ChangeSet(order = "02", author = "initiator", id = "02-addUsers")
    public void addUsers(MongoTemplate mongoTemplate) {
        User adminUser = new User();
        adminUser.setId("admin@orcid.org");
        adminUser.setPassword("$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC");
        adminUser.setFirstName("admin");
        adminUser.setLastName("Administrator");
        adminUser.setEmail("admin@orcid.org");
        adminUser.setActivated(true);
        adminUser.setLangKey("en");
        adminUser.setAdmin(true);
        adminUser.setCreatedBy("admin@orcid.org");
        adminUser.setCreatedDate(Instant.now());
        mongoTemplate.save(adminUser);
    }
}
