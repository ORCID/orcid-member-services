package org.orcid.mp.assertion.client;

import org.orcid.mp.assertion.domain.User;
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

    public User getUser(String loginOrId) {
        return internalUserServiceRestClient.get().uri("/internal/users/" + loginOrId).retrieve().toEntity(User.class).getBody();
    }

}
