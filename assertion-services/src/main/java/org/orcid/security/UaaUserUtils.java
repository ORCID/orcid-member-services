package org.orcid.security;

import org.json.JSONException;
import org.json.JSONObject;
import org.orcid.client.Oauth2ServiceClient;
import org.orcid.service.AssertionsService;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.netflix.hystrix.exception.HystrixRuntimeException;

@Component
public class UaaUserUtils {
    private final Logger log = LoggerFactory.getLogger(UaaUserUtils.class);
    
    @Autowired
    private Oauth2ServiceClient oauth2ServiceClient;

    public String getAuthenticatedUaaUserId() throws JSONException {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BadRequestAlertException("User is not logged in", "login", "null");
        }

        String loggedInUserId = SecurityUtils.getCurrentUserLogin().get();

        if (!SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)) {
            throw new BadRequestAlertException("User does not have the required scope 'AuthoritiesConstants.ASSERTION_SERVICE_ENABLED'", "login", loggedInUserId);
        }

        JSONObject uaaUser = getUAAUser(loggedInUserId);
        
        return uaaUser.getString("id");
    }
    
    private JSONObject getUAAUser(String userLogin) throws JSONException {
        JSONObject existingUaaUser = null;
        try {
            ResponseEntity<String> existingUserResponse = oauth2ServiceClient.getUser(userLogin);
            log.debug("Status code: " + existingUserResponse.getStatusCodeValue());
            if (existingUserResponse != null) {
                existingUaaUser = new JSONObject(existingUserResponse.getBody());
            }
        } catch (HystrixRuntimeException hre) {
            if (hre.getCause() != null && ResponseStatusException.class.isAssignableFrom(hre.getCause().getClass())) {
                ResponseStatusException rse = (ResponseStatusException) hre.getCause();
                if (HttpStatus.NOT_FOUND.equals(rse.getStatus())) {
                    log.debug("User not found: " + userLogin);
                } else {
                    throw hre;
                }
            }
        }
        return existingUaaUser;
    }    
}
