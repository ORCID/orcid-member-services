package org.orcid.memberportal.service.user.config.dbmigrations;

import org.orcid.memberportal.service.user.domain.Authority;
import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.security.AuthoritiesConstants;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;

/**
 * Creates the initial database setup.
 */
@ChangeLog(order = "001")
public class InitialSetupMigration {

    @ChangeSet(order = "01", author = "initiator", id = "01-addAuthorities")
    public void addAuthorities(MongoTemplate mongoTemplate) {
        Authority adminAuthority = new Authority();
        adminAuthority.setName(AuthoritiesConstants.ADMIN);
        Authority userAuthority = new Authority();
        userAuthority.setName(AuthoritiesConstants.USER);
        Authority consortiumLeadAuthority = new Authority();
        consortiumLeadAuthority.setName(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
        mongoTemplate.save(adminAuthority);
        mongoTemplate.save(userAuthority);
        mongoTemplate.save(consortiumLeadAuthority);
    }

    @ChangeSet(order = "02", author = "initiator", id = "02-addUsers")
    public void addUsers(MongoTemplate mongoTemplate) {
        Authority adminAuthority = new Authority();
        adminAuthority.setName(AuthoritiesConstants.ADMIN);
        Authority userAuthority = new Authority();
        userAuthority.setName(AuthoritiesConstants.USER);

        User adminUser = new User();
        adminUser.setId("admin@orcid.org");
        adminUser.setPassword("$2a$10$gSAhZrxMllrbgj/kkK9UceBPpChGWJA7SYIb1Mqo.n5aNLq1/oRrC");
        adminUser.setFirstName("admin");
        adminUser.setLastName("Administrator");
        adminUser.setEmail("admin@orcid.org");
        adminUser.setActivated(true);
        adminUser.setLangKey("en");
        adminUser.setCreatedBy("admin@orcid.org");
        adminUser.setCreatedDate(Instant.now());
        adminUser.getAuthorities().add(adminAuthority.getName());
        adminUser.getAuthorities().add(userAuthority.getName());
        mongoTemplate.save(adminUser);
    }
}
