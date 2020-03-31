package org.orcid.user.security;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.user.client.Oauth2ServiceClient;
import org.orcid.user.web.rest.errors.BadRequestAlertException;
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
    
    @Autowired
    private SecurityUtils securityUtils;

    public String getAuthenticatedUaaUserId() throws JSONException {
        if (!securityUtils.isAuthenticated()) {
            throw new BadRequestAlertException("User is not logged in", "login", "null");
        }

        String loggedInUserId = securityUtils.getCurrentUserLogin().get();
        
        JSONObject uaaUser = getUAAUserByLogin(loggedInUserId);
        
        return uaaUser.getString("id");
    }
    
    public JSONObject getUAAUserByLogin(String login) throws JSONException {
        return getUAAUser(login, false);
    }

    public JSONObject getUAAUserById(String id) throws JSONException {
        return getUAAUser(id, true);
    }

    private JSONObject getUAAUser(String loginOrId, boolean isId) throws JSONException {
        JSONObject existingUaaUser = null;
        try {
            ResponseEntity<String> existingUserResponse = null;
            if (isId) {
                existingUserResponse = oauth2ServiceClient.getUserById(loginOrId);
            } else {
                existingUserResponse = oauth2ServiceClient.getUser(loginOrId);
            }
            if (existingUserResponse != null) {
                existingUaaUser = new JSONObject(existingUserResponse.getBody());
            }
        } catch (HystrixRuntimeException hre) {
            if (hre.getCause() != null && ResponseStatusException.class.isAssignableFrom(hre.getCause().getClass())) {
                ResponseStatusException rse = (ResponseStatusException) hre.getCause();
                if (HttpStatus.NOT_FOUND.equals(rse.getStatus())) {
                    log.debug("User not found: " + loginOrId);
                } else {
                    throw hre;
                }
            }
        }
        return existingUaaUser;
    }
}
