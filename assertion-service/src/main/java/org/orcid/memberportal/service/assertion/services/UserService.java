package org.orcid.memberportal.service.assertion.services;

import java.util.List;

import org.orcid.memberportal.service.assertion.client.UserServiceClient;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;

@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserServiceClient userServiceClient;

    // add cache to this
    public AssertionServiceUser getLoggedInUser() {
        String login = SecurityUtils.getCurrentUserLogin().get();
        ResponseEntity<AssertionServiceUser> userResponse = userServiceClient.getUser(login);
        if (userResponse.getStatusCode().is2xxSuccessful()) {
            return userResponse.getBody();
        }
        LOG.error("No user found in user service for logged in user {}", login);
        throw new IllegalArgumentException("No user found for username" + login);
    }

    public AssertionServiceUser getLoginAsUser(AssertionServiceUser loggedInUser) {
        if (!StringUtils.isAllBlank(loggedInUser.getLoginAs())) {
            ResponseEntity<AssertionServiceUser> userResponse = userServiceClient.getUser(loggedInUser.getLoginAs());
            if (userResponse.getStatusCode().is2xxSuccessful()) {
                return userResponse.getBody();
            }
        }
        LOG.error("No user found in user service for impersonated user for admin {}", loggedInUser.getEmail());
        throw new IllegalArgumentException("No user found for impersonated user for admin" + loggedInUser.getEmail());
    }

    public String getLoggedInUserId() {
        return getLoggedInUser().getId();
    }

    public List<AssertionServiceUser> getUsersBySalesforceId(String salesforceId) {
        ResponseEntity<List<AssertionServiceUser>> response = userServiceClient.getUsersBySalesforceId(salesforceId);
        return response.getBody();
    }

    public String getLoggedInUserSalesforceId() {
        AssertionServiceUser user = getLoggedInUser();
        if (!StringUtils.isAllBlank(user.getLoginAs())) {
            AssertionServiceUser loginAsUser = getLoginAsUser(user);
            return loginAsUser.getSalesforceId();
        } else {
            return user.getSalesforceId();
        }
    }

}
