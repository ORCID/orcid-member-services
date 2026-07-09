package org.orcid.mp.member.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Client for fetching member data from the MP member service.
 */
@Component
public class InternalUserServiceClient {

    @Autowired
    @Qualifier("internalUserServiceRestClient")
    private RestClient internalUserServiceRestClient;

    public String updateUsersMemberNames(String memberId, String newMemberName) {
        return internalUserServiceRestClient.put().uri("/internal/users/memberName/" + memberId + "/" + newMemberName).retrieve().toEntity(String.class).getBody();
    }

}
