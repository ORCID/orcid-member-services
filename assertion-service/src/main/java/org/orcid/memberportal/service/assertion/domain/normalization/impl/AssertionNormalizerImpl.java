package org.orcid.memberportal.service.assertion.domain.normalization.impl;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.normalization.AssertionNormalizer;
import org.orcid.memberportal.service.assertion.domain.normalization.org.OrgNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssertionNormalizerImpl implements AssertionNormalizer {
    
    private static final Logger LOG = LoggerFactory.getLogger(AssertionNormalizerImpl.class);
    
    @Autowired
    private List<OrgNormalizer> orgNormalizers;

    @Override
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
