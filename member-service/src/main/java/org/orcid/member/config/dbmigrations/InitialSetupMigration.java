package org.orcid.member.config.dbmigrations;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.orcid.member.domain.Member;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;

/**
 * Creates the initial database setup.
 */
@ChangeLog(order = "001")
public class InitialSetupMigration {

    @ChangeSet(order = "01", author = "initiator", id = "01-addMembers")
    public void addMembers(MongoTemplate mongoTemplate) {
        Member orcidMember = new Member();
        orcidMember.setId("ORCID, Inc");
        orcidMember.setClientName("ORCID, Inc");
        orcidMember.setSalesforceId("001G000001AP83e");
        orcidMember.setClientId("APP-XFIBOUNATVJIIN7Q");
        orcidMember.setIsConsortiumLead(true);
        orcidMember.setAssertionServiceEnabled(true);
        orcidMember.setSuperadminEnabled(true);
        orcidMember.setCreatedBy("admin@orcid.org");
        orcidMember.setCreatedDate(Instant.now());
        mongoTemplate.save(orcidMember);
    }
}
