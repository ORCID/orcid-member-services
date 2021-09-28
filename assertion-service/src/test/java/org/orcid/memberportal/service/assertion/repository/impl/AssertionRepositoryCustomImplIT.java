package org.orcid.memberportal.service.assertion.repository.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orcid.memberportal.service.assertion.AssertionServiceApp;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.repository.AssertionRepositoryCustom;
import org.orcid.memberportal.service.assertion.repository.impl.AssertionRepositoryCustomImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

@SpringBootTest(classes = { AssertionServiceApp.class })
public class AssertionRepositoryCustomImplIT {

    @Autowired
    private AssertionRepository assertionRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private AssertionRepositoryCustom assertionRepositoryCustom;

    @BeforeEach
    public void setUp() {
        assertionRepositoryCustom = new AssertionRepositoryCustomImpl(mongoTemplate);
        assertionRepository.deleteAll();

        List<Assertion> toCreate = getAssertionsToCreateInOrcid();
        List<Assertion> toUpdate = getAssertionsToUpdateInOrcid();
        List<Assertion> others = getOtherAssertions();

        assertionRepository.saveAll(toCreate);
        assertionRepository.saveAll(toUpdate);
        assertionRepository.saveAll(others);
    }

    @Test
    public void testFindAllToCreateInOrcidRegistry() {
        List<Assertion> toCreate = assertionRepositoryCustom.findAllToCreateInOrcidRegistry();
        assertThat(toCreate.size()).isEqualTo(10);
        toCreate.forEach(a -> assertThat(a.getRoleTitle()).startsWith("create"));
    }

    @Test
    public void testFindAllToUpdateInOrcidRegistry() {
        List<Assertion> toUpdate = assertionRepositoryCustom.findAllToUpdateInOrcidRegistry();
        assertThat(toUpdate.size()).isEqualTo(10);
        toUpdate.forEach(a -> {
            Assertion reloaded = assertionRepository.findById(a.getId()).get();
            assertThat(reloaded.getRoleTitle()).startsWith("update");
        });
    }

    private List<Assertion> getAssertionsToUpdateInOrcid() {
        List<Assertion> assertions = new ArrayList<Assertion>();
        for (int i = 0; i < 10; i++) {
            assertions.add(getAssertionToUpdateInOrcid(i));
        }
        return assertions;
    }

    private List<Assertion> getAssertionsToCreateInOrcid() {
        List<Assertion> assertions = new ArrayList<Assertion>();
        for (int i = 0; i < 10; i++) {
            assertions.add(getAssertionToCreateInOrcid(i));
        }
        return assertions;
    }

    private List<Assertion> getOtherAssertions() {
        List<Assertion> assertions = new ArrayList<Assertion>();
        for (int i = 0; i < 10; i++) {
            assertions.add(getOtherAssertion(i));
        }
        return assertions;
    }

    private Assertion getAssertionToUpdateInOrcid(int i) {
        Assertion assertion = getAssertion(i);
        assertion.setId("update " + i);
        assertion.setRoleTitle("update " + i);
        assertion.setAddedToORCID(Instant.now());
        assertion.setUpdatedInORCID(Instant.now().plusSeconds(1l));
        assertion.setModified(Instant.now().plusSeconds(10l));
        return assertion;
    }

    private Assertion getAssertionToCreateInOrcid(int i) {
        Assertion assertion = getAssertion(i);
        assertion.setId("create " + i);
        assertion.setRoleTitle("create " + i);
        assertion.setModified(Instant.now());
        return assertion;
    }

    private Assertion getOtherAssertion(int i) {
        Assertion assertion = getAssertion(i);
        assertion.setId("other " + i);
        assertion.setRoleTitle("other " + i);
        assertion.setAddedToORCID(Instant.now());
        assertion.setModified(Instant.now().plusSeconds(1l));
        assertion.setUpdatedInORCID(Instant.now().plusSeconds(10l));
        return assertion;
    }

    private Assertion getAssertion(int i) {
        Assertion assertion = new Assertion();
        assertion.setOrgName("org name");
        assertion.setDisambiguatedOrgId("id");
        assertion.setAffiliationSection(AffiliationSection.DISTINCTION);
        assertion.setOrgCity("city");
        assertion.setOrgCountry("US");
        assertion.setEmail("email@orcid.org");
        return assertion;
    }

}
