package org.orcid.memberportal.service.member.config.dbmigrations;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;

import org.orcid.memberportal.service.member.domain.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;

/**
 * Creates the initial database setup.
 */
@ChangeLog(order = "001")
public class InitialSetupMigration {
    private static final Logger LOG = LoggerFactory.getLogger(InitialSetupMigration.class);

    @ChangeSet(order = "01", author = "initiator", id = "01-addMembers")
    public void addMembers(MongoTemplate mongoTemplate, Environment environment) {
        Member orcidMember = new Member();
        String salesForceId = "001G000001AP83e";

        if (environment.getProperty("application_orcidOrgSalesForceId") != null) {
            salesForceId = environment.getProperty("application.orcidOrgSalesForceId");
        }
        String clientId = "APP-1ERTY7037V1I8FE5";
        if (environment.getProperty("application.orcidOrgClientId") != null) {
            clientId = environment.getProperty("application.orcidOrgClientId");
        }
        orcidMember.setId("ORCID, Inc");
        orcidMember.setClientName("ORCID, Inc");
        orcidMember.setSalesforceId(salesForceId);
        orcidMember.setClientId(clientId);
        orcidMember.setIsConsortiumLead(true);
        orcidMember.setAssertionServiceEnabled(true);
        orcidMember.setSuperadminEnabled(true);
        orcidMember.setCreatedBy("admin@orcid.org");
        orcidMember.setCreatedDate(Instant.now());
        mongoTemplate.save(orcidMember);
    }

}
