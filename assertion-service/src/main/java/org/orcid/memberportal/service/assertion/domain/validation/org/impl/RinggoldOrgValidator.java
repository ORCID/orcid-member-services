package org.orcid.memberportal.service.assertion.domain.validation.org.impl;

import org.orcid.memberportal.service.assertion.domain.validation.org.OrgValidator;
import org.springframework.stereotype.Component;

@Component
public class RinggoldOrgValidator implements OrgValidator {

    @Override
    public boolean validId(String id) {
        return id.chars().allMatch(x -> Character.isDigit(x));
    }
    
    

}
