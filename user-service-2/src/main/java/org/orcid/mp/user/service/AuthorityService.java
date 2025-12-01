package org.orcid.mp.user.service;


import org.orcid.mp.user.client.MemberServiceClient;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.security.AuthoritiesConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AuthorityService {

    @Autowired
    private MemberServiceClient memberServiceClient;

    public Set<String> getAuthoritiesForUser(User user) {
        Set<String> authorities = Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet());
        if (user.getMainContact() != null && user.getMainContact().booleanValue()) {
            authorities.add(AuthoritiesConstants.ORG_OWNER);
        }

        if (!org.apache.commons.lang3.StringUtils.isBlank(user.getSalesforceId())) {
            Member member = memberServiceClient.getMember(user.getSalesforceId());
            if (member != null) {
                if (member.getAssertionServiceEnabled()) {
                    authorities.add(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
                }
                if (member.getIsConsortiumLead()) {
                    authorities.add(AuthoritiesConstants.CONSORTIUM_LEAD);
                }
                if (user.getAdmin() != null && user.getAdmin().booleanValue() && member.getSuperadminEnabled()) {
                    authorities.add(AuthoritiesConstants.ADMIN);
                }
            }
        }
        return authorities;
    }

}
