package org.orcid.member.config.dbmigrations;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;

import org.orcid.member.domain.Member;
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
    public void addMembers(MongoTemplate mongoTemplate, Environment environment)  {    	
        Member orcidMember = new Member();
        orcidMember.setId("ORCID, Inc");
        orcidMember.setClientName("ORCID, Inc");
        orcidMember.setSalesforceId(environment.getProperty("application.orcidOrgSalesForceId"));    
        orcidMember.setClientId(environment.getProperty("application.orcidOrgClientId"));
        orcidMember.setIsConsortiumLead(true);
        orcidMember.setAssertionServiceEnabled(true);
        orcidMember.setSuperadminEnabled(true);
        orcidMember.setCreatedBy("admin@orcid.org");
        orcidMember.setCreatedDate(Instant.now());
        mongoTemplate.save(orcidMember);
    }
   
    
}
