package org.orcid.service;

import org.orcid.client.UserServiceClient;
import org.orcid.domain.AssertionsUser;
import org.orcid.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AssertionsUserService {

	private static final Logger LOG = LoggerFactory.getLogger(AssertionsUserService.class);
	
	@Autowired
	private UserServiceClient userServiceClient;
	
	// add cache to this
	public AssertionsUser getLoggedInUser() {
		String login = SecurityUtils.getCurrentUserLogin().get();
		ResponseEntity<AssertionsUser> userResponse = userServiceClient.getUser(login);
		if (userResponse.getStatusCode().is2xxSuccessful()) {
			return userResponse.getBody();
		}
		LOG.error("No user found in user service for logged in user {}", login);
		throw new IllegalArgumentException("No user found for username" + login);
	}
	
	public String getLoggedInUserId() {
		return getLoggedInUser().getId();
	}
	
}
