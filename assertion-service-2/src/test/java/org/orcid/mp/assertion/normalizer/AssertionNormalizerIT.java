package org.orcid.mp.assertion.normalizer;

import org.junit.jupiter.api.Test;
import org.orcid.mp.assertion.AssertionServiceApplication;
import org.orcid.mp.assertion.config.Constants;
import org.orcid.mp.assertion.domain.Assertion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AssertionServiceApplication.class)
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