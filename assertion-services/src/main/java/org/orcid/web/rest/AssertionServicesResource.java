package org.orcid.web.rest;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.client.UserSettingsClient;
import org.orcid.domain.Affiliation;
import org.orcid.security.AuthoritiesConstants;
import org.orcid.security.SecurityUtils;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assertion/api")
public class AssertionServicesResource {
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private UserSettingsClient userSettingsClient;

    private String getAuthenticatedUser() {
        if (!SecurityUtils.isAuthenticated()) {
            throw new BadRequestAlertException("User is not logged in", "login", "null");
        }

        String loggedInUser = SecurityUtils.getCurrentUserLogin().get();

        if (!SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)) {
            throw new BadRequestAlertException("User does not have the required scope 'AuthoritiesConstants.ASSERTION_SERVICE_ENABLED'", "login", loggedInUser);
        }

        return loggedInUser;
    }

    @GetMapping("/affiliation/{id}")
    public ResponseEntity<String> getAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {
        String loggedInUser = getAuthenticatedUser();

        ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(loggedInUser);

        JSONObject userSettings = new JSONObject(userSettingsResponse.getBody());
        String firstName = userSettings.getString("firstName");
        String lastName = userSettings.getString("lastName");
        String salesforceId = userSettings.getString("salesforceId");

        return ResponseEntity.ok().body(StringUtils.join("getAssertions", firstName, lastName, salesforceId));
    }
    
    @PutMapping("/affiliation")
    public ResponseEntity<String> updateAssertion(@RequestBody Affiliation assertion) throws BadRequestAlertException, JSONException {
        String loggedInUser = getAuthenticatedUser();

        ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(loggedInUser);

        JSONObject userSettings = new JSONObject(userSettingsResponse.getBody());
        String firstName = userSettings.getString("firstName");
        String lastName = userSettings.getString("lastName");
        String salesforceId = userSettings.getString("salesforceId");

        return ResponseEntity.ok().body(StringUtils.join("getAssertions", firstName, lastName, salesforceId));
    }
    
    @PostMapping("/affiliation")
    public ResponseEntity<String> createAssertion(@RequestBody Affiliation assertion) throws BadRequestAlertException, JSONException {
        String loggedInUser = getAuthenticatedUser();

        ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(loggedInUser);

        JSONObject userSettings = new JSONObject(userSettingsResponse.getBody());
        String firstName = userSettings.getString("firstName");
        String lastName = userSettings.getString("lastName");
        String salesforceId = userSettings.getString("salesforceId");

        return ResponseEntity.ok().body(StringUtils.join("getAssertions", firstName, lastName, salesforceId));
    }
    
    @DeleteMapping("/affiliation/{id}")
    public ResponseEntity<String> deleteAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {
        String loggedInUser = getAuthenticatedUser();

        ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(loggedInUser);

        JSONObject userSettings = new JSONObject(userSettingsResponse.getBody());
        String firstName = userSettings.getString("firstName");
        String lastName = userSettings.getString("lastName");
        String salesforceId = userSettings.getString("salesforceId");

        return ResponseEntity.ok().body(StringUtils.join("getAssertions", firstName, lastName, salesforceId));
    }
}
