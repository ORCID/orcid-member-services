package org.orcid.member.service;

import java.util.List;

import org.orcid.member.client.UserServiceClient;
import org.orcid.member.security.SecurityUtils;
import org.orcid.member.service.user.MemberServiceUser;
import org.orcid.member.web.rest.errors.BadRequestAlertException;
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
    public MemberServiceUser getLoggedInUser() {
        String login = SecurityUtils.getCurrentUserLogin().get();
        ResponseEntity<MemberServiceUser> userResponse = userServiceClient.getUser(login);
        if (userResponse.getStatusCode().is2xxSuccessful()) {
            return userResponse.getBody();
        }
        LOG.error("No user found in user service for logged in user {}", login);
        throw new IllegalArgumentException("No user found for username" + login);
    }

    public MemberServiceUser getImpersonatedUser() {
        MemberServiceUser loggedInUser = getLoggedInUser();
        if (!StringUtils.isAllBlank(loggedInUser.getLoginAs())) {
            ResponseEntity<MemberServiceUser> userResponse = userServiceClient.getUser(loggedInUser.getLoginAs());
            if (userResponse.getStatusCode().is2xxSuccessful()) {
                return userResponse.getBody();
            }
        }
        LOG.error("No impersonated user found");
        throw new IllegalArgumentException("No impersonated user found");
    }

    public String getLoggedInUserId() {
        return getLoggedInUser().getId();
    }

    public List<MemberServiceUser> getUsersBySalesforceId(String salesforceId) {
        ResponseEntity<List<MemberServiceUser>> response = userServiceClient.getUsersBySalesforceId(salesforceId);
        return response.getBody();
    }

    public void updateUser(MemberServiceUser user) {
        ResponseEntity<String> response = userServiceClient.updateUser(user);
        if (!response.getStatusCode().is2xxSuccessful()) {
            LOG.warn("Error updating user {}, response code {}", user.getLogin(), response.getStatusCodeValue());
            throw new BadRequestAlertException("Unable to update user", "User", null);
        }
    }

    public void updateUserSalesforceIdOrAssertion(String salesforceId, String newSalesforceId) {
        ResponseEntity<String> response = userServiceClient.updateUserSalesforceIdOrAssertion(salesforceId,
                newSalesforceId);
        if (!response.getStatusCode().is2xxSuccessful()) {
            LOG.warn("Error updating users {}, response code {}", salesforceId, response.getStatusCodeValue());
            throw new BadRequestAlertException("Unable to update users", "User", null);
        }
    }

    public String getSalesforceIdForUser(String userId) {
        ResponseEntity<MemberServiceUser> response = userServiceClient.getUser(userId);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody().getSalesforceId();
        } else {
            return null;
        }
    }

    public void deleteUserById(String loginOrId, boolean noMainContactCheck) {
        ResponseEntity<Void> response = userServiceClient.deleteUser(loginOrId, noMainContactCheck);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BadRequestAlertException("Unable to delete user", "User", null);
        }
    }

}
