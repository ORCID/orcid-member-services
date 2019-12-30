package org.orcid.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.orcid.client.UserSettingsClient;
import org.orcid.domain.Assertion;
import org.orcid.repository.AffiliationsRepository;
import org.orcid.security.AuthoritiesConstants;
import org.orcid.security.SecurityUtils;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class AssertionServicesResource {
    private static final String ENTITY_NAME = "affiliation";
    
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private UserSettingsClient userSettingsClient;
    
    private final AffiliationsRepository affiliationsRepository;

    public AssertionServicesResource(AffiliationsRepository affiliationsRepository) {
        this.affiliationsRepository = affiliationsRepository;
    }
    
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

    @GetMapping("/assertions")
    public ResponseEntity<List<Assertion>> getAssertions(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder) throws BadRequestAlertException, JSONException {
        String loggedInUserId = getAuthenticatedUser();

        Page<Assertion> affiliations = affiliationsRepository.findByOwnerId(loggedInUserId, pageable);
        
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), affiliations);
        return ResponseEntity.ok().headers(headers).body(affiliations.getContent());
    }
    
    @GetMapping("/assertion/{id}")
    public ResponseEntity<String> getAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {
        //TODO
        String loggedInUser = getAuthenticatedUser();

        ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(loggedInUser);

        JSONObject userSettings = new JSONObject(userSettingsResponse.getBody());
        String firstName = userSettings.getString("firstName");
        String lastName = userSettings.getString("lastName");
        String salesforceId = userSettings.getString("salesforceId");

        return ResponseEntity.ok().body(StringUtils.join("getAssertions", firstName, lastName, salesforceId));
    }
    
    @PutMapping("/assertion")
    public ResponseEntity<String> updateAssertion(@RequestBody Assertion assertion) throws BadRequestAlertException, JSONException {
        //TODO
        String loggedInUser = getAuthenticatedUser();

        ResponseEntity<String> userSettingsResponse = userSettingsClient.getUserSettings(loggedInUser);

        JSONObject userSettings = new JSONObject(userSettingsResponse.getBody());
        String firstName = userSettings.getString("firstName");
        String lastName = userSettings.getString("lastName");
        String salesforceId = userSettings.getString("salesforceId");

        return ResponseEntity.ok().body(StringUtils.join("getAssertions", firstName, lastName, salesforceId));
    }
    
    @PostMapping("/assertion")
    public ResponseEntity<Assertion> createAssertion(@Valid @RequestBody Assertion assertion) throws BadRequestAlertException, JSONException, URISyntaxException {
        String loggedInUser = getAuthenticatedUser();

        Instant now = Instant.now();
        
        assertion.setOwnerId(loggedInUser);
        assertion.setCreated(now);
        assertion.setModified(now);

        assertion = affiliationsRepository.save(assertion);
        
        return ResponseEntity.created(new URI("/api/assertion/" + assertion.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, assertion.getId())).body(assertion);
    }
    
    private void validateAssertion(Assertion assertion) {
        if (StringUtils.isBlank(assertion.getEmail())) {
            throw new IllegalArgumentException("email must not be null");
        }
        
        if (assertion.getAffiliationSection() == null) {
            throw new IllegalArgumentException("affiliation-section must not be null");
        }
        if (StringUtils.isBlank(assertion.getOrgName())) {
            throw new IllegalArgumentException("org-name must not be null");
        }        
        if (StringUtils.isBlank(assertion.getOrgCountry())) {
            throw new IllegalArgumentException("org-country must not be null");
        } 
        if (StringUtils.isBlank(assertion.getOrgCity())) {
            throw new IllegalArgumentException("org-city must not be null");
        }
        if (StringUtils.isBlank(assertion.getDisambiguatedOrgId())) {
            throw new IllegalArgumentException("disambiguated-organization-identifier must not be null");
        }
        if (StringUtils.isBlank(assertion.getDisambiguationSource())) {
            throw new IllegalArgumentException("disambiguation-source must not be null");
        }        
    }
    
    @DeleteMapping("/assertion/{id}")
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
