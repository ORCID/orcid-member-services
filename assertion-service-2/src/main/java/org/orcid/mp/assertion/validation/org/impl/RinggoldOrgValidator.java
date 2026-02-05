package org.orcid.mp.assertion.validation.org.impl;

import org.orcid.mp.assertion.validation.org.OrgValidator;
import org.springframework.stereotype.Component;

@Component
public class RinggoldOrgValidator implements OrgValidator {

    @Override
    public boolean validId(String id) {
        return id.chars().allMatch(x -> Character.isDigit(x));
    }

}