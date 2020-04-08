package org.orcid.user.service.impl;

import java.util.Optional;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.user.client.Oauth2ServiceClient;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.netflix.hystrix.exception.HystrixRuntimeException;

/**
 * Central place for logic for jhi_users and user_settings.
 * 
 * @author georgenash
 *
 */
@Service
public class UserServiceImpl implements UserService {
	
	private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);
	
	@Autowired
    private UserSettingsRepository userSettingsRepository;
	
	@Autowired
    private Oauth2ServiceClient oauth2ServiceClient;

	@Override
	public boolean canCreateUser(String login) {
		ResponseEntity<String> user = null;
		String jhiUserId = null;
		
		try {
            user = oauth2ServiceClient.getUser(login);
        } catch (HystrixRuntimeException hre) {
            if (hre.getCause() != null && ResponseStatusException.class.isAssignableFrom(hre.getCause().getClass())) {
                ResponseStatusException rse = (ResponseStatusException) hre.getCause();
                if (HttpStatus.NOT_FOUND.equals(rse.getStatus())) {
                    return true;
                }
            }
        }
		
		try {
			JSONObject userJSON = new JSONObject(user.getBody());
			jhiUserId = userJSON.getString("id");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		
		Optional<UserSettings> userSettings = userSettingsRepository.findByJhiUserId(jhiUserId);
		if (userSettings.isPresent() && userSettings.get().getDeleted()) {
			return true;
		}
		
		return false;
	}

}
