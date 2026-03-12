package org.orcid.mp.assertion.client;

import org.orcid.mp.assertion.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

}
