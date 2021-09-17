package org.orcid.memberportal.service.assertion.org.validation.impl;

import org.orcid.memberportal.service.assertion.org.validation.OrgValidator;
import org.springframework.stereotype.Component;

@Component
public class RinggoldOrgValidator implements OrgValidator {

    @Override
    public boolean validId(String id) {
        return id.chars().allMatch(x -> Character.isDigit(x));
    }
    
    

}
