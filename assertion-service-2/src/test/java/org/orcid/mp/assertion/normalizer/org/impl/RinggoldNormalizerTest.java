package org.orcid.mp.assertion.normalizer.org.impl;

import org.junit.jupiter.api.Test;
import org.orcid.mp.assertion.config.Constants;

import static org.assertj.core.api.Assertions.assertThat;

public class RinggoldNormalizerTest {

    private final RinggoldNormalizer normalizer = new RinggoldNormalizer();

    @Test
    public void testGetOrgSource() {
        assertThat(normalizer.getOrgSource()).isEqualTo(Constants.RINGGOLD_ORG_SOURCE);
    }

    @Test
    public void testNormalizeOrgId() {
        assertThat(normalizer.normalizeOrgId("test")).isEqualTo("test");
    }

}
