package org.orcid.memberportal.service.assertion.domain.normalization.org.impl;

import org.junit.jupiter.api.Test;
import org.orcid.memberportal.service.assertion.config.Constants;

import static org.assertj.core.api.Assertions.assertThat;

public class RorNormalizerTest {

    private RorNormalizer normalizer = new RorNormalizer();

    @Test
    public void testGetOrgSource() {
        assertThat(normalizer.getOrgSource()).isEqualTo(Constants.ROR_ORG_SOURCE);
    }
    
    @Test
    public void testNormalizeOrgId() {
        assertThat(normalizer.normalizeOrgId("03yrm5c26")).isEqualTo("https://ror.org/03yrm5c26");
        assertThat(normalizer.normalizeOrgId("https://ror.org/03yrm5c26")).isEqualTo("https://ror.org/03yrm5c26");
    }

}
