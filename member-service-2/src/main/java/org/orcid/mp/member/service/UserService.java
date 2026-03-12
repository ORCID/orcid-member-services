package org.orcid.mp.member.service;

import java.util.List;

import org.orcid.mp.member.client.UserServiceClient;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserServiceClient userServiceClient;

    public User getLoggedInUser() {
        String login = SecurityUtils.getCurrentUserLogin().get();
        LOG.info("Logged in user is {}", login);
        return userServiceClient.getUser(login);
    }

    public String getLoggedInUserId() {
        return getLoggedInUser().getId();
    }

    public List<User> getUsersBySalesforceId(String salesforceId) {
        return userServiceClient.getUsersBySalesforceId(salesforceId);
    }

    public void updateUser(User user) {
        userServiceClient.updateUser(user);
    }

    public String getSalesforceIdForUser(String userId) {
        User user = userServiceClient.getUser(userId);
        return user.getSalesforceId();
    }

    public void updateUsersMemberNames(String salesforceId, String newClientName) {
        userServiceClient.updateUsersMemberNames(salesforceId, newClientName);
    }
}
