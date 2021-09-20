package org.orcid.memberportal.service.assertion.domain.validation.org.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orcid.memberportal.service.assertion.domain.validation.org.OrgValidator;
import org.springframework.stereotype.Component;

@Component
public class RorOrgValidator implements OrgValidator {

    private static final Pattern ROR_ID_PATTERN = Pattern.compile("^(https://ror.org/)?0[^ILO]{6}\\d{2}$");
    
    @Override
    public boolean validId(String id) {
        Matcher matcher = ROR_ID_PATTERN.matcher(id);
        return matcher.find();
    }
    
    

}
