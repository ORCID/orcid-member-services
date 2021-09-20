package org.orcid.memberportal.service.assertion.domain.normalization.org.impl;

import org.orcid.memberportal.service.assertion.config.Constants;
import org.orcid.memberportal.service.assertion.domain.normalization.org.OrgNormalizer;
import org.springframework.stereotype.Component;

@Component
public class RorNormalizer implements OrgNormalizer {
    
    private static final String ROR_URL_BASE = "https://ror.org/";
    
    @Override
    public String normalizeOrgId(String orgId) {
        if (!orgId.startsWith(ROR_URL_BASE)) {
            orgId = ROR_URL_BASE + orgId;
        }
        return orgId;
    }

    @Override
    public String getOrgSource() {
        return Constants.ROR_ORG_SOURCE;
    }

}
