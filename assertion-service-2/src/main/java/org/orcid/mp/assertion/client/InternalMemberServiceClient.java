package org.orcid.mp.assertion.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import org.orcid.mp.assertion.domain.Member;

/**
 * Client for fetching member data from the MP member service.
 */
@Component
public class InternalMemberServiceClient {

    @Autowired
    @Qualifier("internalMemberServiceRestClient")
    private RestClient internalMemberServiceRestClient;

    // used by getMemberAssertionStats backend job
    public Member getMember(String id) {
        return internalMemberServiceRestClient.get().uri("/internal/members/" + id).retrieve().toEntity(Member.class).getBody();
    }

}
