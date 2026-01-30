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
public class InternalMemberServiceClient {

    @Autowired
    @Qualifier("internalMemberServiceRestClient")
    private RestClient internalMemberServiceRestClient;

    public Member getMember(String id) {
        return internalMemberServiceRestClient.get().uri("/internal/members/" + id).retrieve().toEntity(Member.class).getBody();
    }

}
