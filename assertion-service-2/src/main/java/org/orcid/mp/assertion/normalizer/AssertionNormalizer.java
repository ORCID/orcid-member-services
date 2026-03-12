package org.orcid.mp.assertion.normalizer;

import java.util.List;

import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.normalizer.org.OrgNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssertionNormalizer {

    private static final Logger LOG = LoggerFactory.getLogger(AssertionNormalizer.class);

    @Autowired
    private List<OrgNormalizer> orgNormalizers;

    public Assertion normalize(Assertion assertion) {
        OrgNormalizer normalizer = getMatchingNormalizer(assertion.getDisambiguationSource());
        if (normalizer != null) {
            assertion.setDisambiguatedOrgId(normalizer.normalizeOrgId(assertion.getDisambiguatedOrgId()));
        }
        return assertion;
    }

    private OrgNormalizer getMatchingNormalizer(String orgType) {
        for (OrgNormalizer normalizer : orgNormalizers) {
            if (orgType.equals(normalizer.getOrgSource())) {
                return normalizer;
            }
        }
        LOG.warn("No normalizer found for org type {}", orgType);
        return null;
    }

}