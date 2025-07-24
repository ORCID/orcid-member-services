package org.orcid.memberportal.service.assertion.scripts;

import org.orcid.memberportal.service.assertion.AssertionServiceApp;
import org.orcid.memberportal.service.assertion.config.*;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.OrcidToken;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.repository.OrcidRecordRepository;
import org.orcid.memberportal.service.assertion.services.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Optional;

public class ExamineAssertionsMissingOrcid {

    private static final Logger LOG = LoggerFactory.getLogger(ExamineAssertionsMissingOrcid.class);

    private static int missingTokenCount = 0;

    private static int matchingTokenCount = 0;

    private static int revokedTokenCount = 0;

    private static int salesforceIdSubstrings = 0;

    private static int missingOrcidRecordCount = 0;

    public static void main(String[] args) {
        // to avoid startup errors involving user service client
        System.setProperty("jhipster.security.client-authorization.client-id", "not needed");

        SpringApplication application = new SpringApplication(AssertionServiceApp.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        ApplicationContext context = application.run(args);

        AssertionRepository assertionRepository = context.getBean(AssertionRepository.class);
        OrcidRecordRepository orcidRecordRepository = context.getBean(OrcidRecordRepository.class);

        List<Assertion> assertions = assertionRepository.findByStatusAndOrcidIdIsNull(AssertionStatus.IN_ORCID.name());
        LOG.info("Found {} IN_ORCID assertions with no orcid id", assertions.size());

        assertions.forEach(a -> {
            Optional<OrcidRecord> optional = orcidRecordRepository.findOneByEmail(a.getEmail());
            if (optional.isEmpty()) {
                LOG.info("No orcid record found for assertion {} - email {}, salesforce id {}", a.getId(), a.getEmail(), a.getSalesforceId());
                missingOrcidRecordCount++;
            } else {
                OrcidRecord orcidRecord = optional.get();
                if (orcidRecord.getToken(a.getSalesforceId(), false) != null) {
                    LOG.info("Found matching token for assertion {} - email {}, salesforce id {}", a.getId(), a.getEmail(), a.getSalesforceId());
                    matchingTokenCount++;
                } else if (orcidRecord.getToken(a.getSalesforceId(), true) != null) {
                    LOG.info("Found REVOKED token for assertion {} - email {}, salesforce id {}", a.getId(), a.getEmail(), a.getSalesforceId());
                    revokedTokenCount++;
                } else {
                    LOG.info("No token found for assertion {} - email {}, salesforce id {}", a.getId(), a.getEmail(), a.getSalesforceId());
                    missingTokenCount++;

                    List<OrcidToken> possibleMatches = orcidRecord.getTokens();
                    possibleMatches.forEach(t -> {
                        if (a.getSalesforceId().contains(t.getSalesforceId())) {
                            LOG.info("Found likely match. Token salesforce id might need updating from {} to {}", t.getSalesforceId(), a.getSalesforceId());
                            salesforceIdSubstrings++;
                        }
                    });
                }
            }
        });

        LOG.info("------ SUMMARY -------");
        LOG.info("Found {} IN_ORCID assertions with no orcid id", assertions.size());
        LOG.info("Found {} missing OrcidRecord", missingOrcidRecordCount);
        LOG.info("Found {} with OrcidRecord and matching token", matchingTokenCount);
        LOG.info("Found {} with revoked matching token", revokedTokenCount);
        LOG.info("Found {} with no matching token", missingTokenCount);
        LOG.info("Found {} possible broken salesforce id matches", salesforceIdSubstrings);

        System.exit(0);
    }


}
