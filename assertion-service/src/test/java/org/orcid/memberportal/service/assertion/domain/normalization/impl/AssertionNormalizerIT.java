package org.orcid.memberportal.service.assertion.domain.normalization.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.orcid.memberportal.service.assertion.AssertionServiceApp;
import org.orcid.memberportal.service.assertion.config.Constants;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.normalization.AssertionNormalizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AssertionServiceApp.class)
public class AssertionNormalizerIT {

    @Autowired
    private AssertionNormalizer assertionNormalizer;
    
    @Test
    public void testNormalize() {
        Assertion normalized = assertionNormalizer.normalize(getAssertionToNormalize());
        assertThat(normalized.getDisambiguatedOrgId()).isEqualTo("https://ror.org/03yrm5c26");
        
        // check that ids with base url aren't changed by normalizer
        normalized = assertionNormalizer.normalize(normalized);
        assertThat(normalized.getDisambiguatedOrgId()).isEqualTo("https://ror.org/03yrm5c26");
    }
    
    private Assertion getAssertionToNormalize() {
        Assertion assertion = new Assertion();
        assertion.setDisambiguatedOrgId("03yrm5c26");
        assertion.setDisambiguationSource(Constants.ROR_ORG_SOURCE);
        assertion.setRoleTitle("testing normalization");
        return assertion;
    }
    
}


