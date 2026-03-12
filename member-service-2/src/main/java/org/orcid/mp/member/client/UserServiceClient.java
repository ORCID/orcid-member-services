package org.orcid.mp.member.client;

import org.orcid.mp.member.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class UserServiceClient {

    @Autowired
    @Qualifier("userServiceRestClient")
    private RestClient restClient;

    public User getUser(String loginOrId) {
        return restClient.get().uri("/users/" + loginOrId).retrieve().toEntity(User.class).getBody();
    }

    public List<User> getUsersBySalesforceId(String salesforceId) {
        return restClient.get().uri("/users/salesforce/" + salesforceId).retrieve().body(new ParameterizedTypeReference<List<User>>() {
        });
    }

    public String updateUser(User user) {
        return restClient.put().uri("/users").body(user).retrieve().toEntity(String.class).getBody();
    }

    public String updateUsersMemberNames(String salesforceId, String newMemberName) {
        return restClient.put().uri("/users/memberName/" + salesforceId + "/" + newMemberName).retrieve().toEntity(String.class).getBody();
    }
}
