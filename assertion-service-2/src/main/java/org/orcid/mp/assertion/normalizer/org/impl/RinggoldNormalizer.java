package org.orcid.mp.assertion.normalizer.org.impl;

import org.orcid.mp.assertion.config.Constants;
import org.orcid.mp.assertion.normalizer.org.OrgNormalizer;
import org.springframework.stereotype.Component;

@Component
public class RinggoldNormalizer implements OrgNormalizer {

    @Override
    public String normalizeOrgId(String orgId) {
        return orgId;
    }

    @Override
    public String getOrgSource() {
        return Constants.RINGGOLD_ORG_SOURCE;
    }

}