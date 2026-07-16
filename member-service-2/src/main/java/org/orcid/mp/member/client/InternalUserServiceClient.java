package org.orcid.mp.member.client;

import org.orcid.mp.member.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

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

    public String createMainContactUser(User user) {
        return internalUserServiceRestClient.post().uri("/internal/users").body(user).retrieve().toEntity(String.class).getBody();
    }

    public List<User> getUsersByMemberId(String memberId) {
        return internalUserServiceRestClient.get().uri("/internal/users/member/" + memberId).retrieve().body(new ParameterizedTypeReference<List<User>>() {
        });
    }
}
