package org.orcid.memberportal.service.assertion.domain.org.validation.impl;

import org.apache.commons.lang3.StringUtils;
import org.orcid.memberportal.service.assertion.domain.org.validation.OrgValidator;
import org.springframework.stereotype.Component;

@Component
public class GridOrgValidator implements OrgValidator {

    private static final String GRID_PREFIX = "grid.";

    @Override
    public boolean validId(String id) {
        return id.length() > (GRID_PREFIX.length() + 1) && StringUtils.equals(id.substring(0, GRID_PREFIX.length()), GRID_PREFIX);
    }

}
