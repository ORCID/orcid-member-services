package org.orcid.mp.user.client;

import org.orcid.mp.user.domain.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for fetching member data from the MP member service.
 */
@Component
public class MemberServiceClient {

    @Autowired
    @Qualifier("memberServiceRestClient")
    private RestClient restClient;

    @Value("${application.memberService.apiUrl}")
    private String memberServiceApiUrl;

    public Member getMember(String id) {
        return restClient.get().uri(memberServiceApiUrl + "/members/" + id).retrieve().toEntity(Member.class).getBody();
    }

    private Member getMockMember() {
        Member member = new Member();
        member.setAssertionServiceEnabled(true);
        member.setSuperadminEnabled(true);
        member.setSalesforceId("salesforce-id");
        member.setIsConsortiumLead(false);
        member.setParentSalesforceId(null);
        return member;
    }

}
