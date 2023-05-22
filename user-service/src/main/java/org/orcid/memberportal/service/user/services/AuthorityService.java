package org.orcid.memberportal.service.user.services;

import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.security.AuthoritiesConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthorityService {

    @Autowired
    private MemberService memberService;

    public Set<String> getAuthoritiesForUser(User user) {
        Set<String> authorities = Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet());
        if (!org.apache.commons.lang3.StringUtils.isBlank(user.getSalesforceId())) {
            if (memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(user.getSalesforceId())) {
                authorities.add(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
            }

            if (memberService.memberIsConsortiumLead(user.getSalesforceId())) {
                authorities.add(AuthoritiesConstants.CONSORTIUM_LEAD);
            }
        }

        if (user.getMainContact() != null && user.getMainContact().booleanValue()) {
            authorities.add(AuthoritiesConstants.ORG_OWNER);
        }

        if (user.getAdmin() != null && user.getAdmin().booleanValue() && memberService.memberIsAdminEnabled(user.getSalesforceId())) {
            authorities.add(AuthoritiesConstants.ADMIN);
        }
        return authorities;
    }

}
