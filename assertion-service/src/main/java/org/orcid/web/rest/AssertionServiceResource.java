package org.orcid.web.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.codehaus.jettison.json.JSONException;
import org.json.JSONObject;
import org.orcid.domain.Assertion;
import org.orcid.domain.AssertionServiceUser;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.validation.OrcidUrlValidator;
import org.orcid.security.AuthoritiesConstants;
import org.orcid.security.EncryptUtil;
import org.orcid.security.JWTUtil;
import org.orcid.security.SecurityUtils;
import org.orcid.service.AssertionService;
import org.orcid.service.OrcidRecordService;
import org.orcid.service.UserService;
import org.orcid.service.assertions.upload.AssertionsUpload;
import org.orcid.service.assertions.upload.impl.AssertionsCsvReader;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.orcid.web.rest.errors.EmailAlreadyUsedException;
import org.orcid.web.rest.errors.ORCIDAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.SignedJWT;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class AssertionServiceResource {
    private static final Logger LOG = LoggerFactory.getLogger(AssertionServiceResource.class);

    private static final String ENTITY_NAME = "affiliation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private AssertionService assertionsService;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private AssertionsCsvReader assertionsCsvReader;
    
    @Autowired
    private UserService assertionsUserService;

    String[] urlValschemes = { "http", "https", "ftp" }; // DEFAULT schemes =
    // "http", "https",
    // "ftp"

    UrlValidator urlValidator = new OrcidUrlValidator(urlValschemes);
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY_MM_dd");

    @GetMapping("/assertions")
    public ResponseEntity<List<Assertion>> getAssertions(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder)
            throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch assertions from user {}", SecurityUtils.getCurrentUserLogin().get());

        Page<Assertion> affiliations = assertionsService.findBySalesforceId(pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), affiliations);
        return ResponseEntity.ok().headers(headers).body(affiliations.getContent());
    }

    @GetMapping("/assertions/{email}")
    public ResponseEntity<List<Assertion>> getAssertionsByEmail(@PathVariable String email)
            throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch assertions for email {}", email);
        List<Assertion> assertions = assertionsService.findByEmail(email);
        return ResponseEntity.ok().body(assertions);
    }

    @GetMapping("/assertion/{id}")
    public ResponseEntity<Assertion> getAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch assertion {} from user {}", id, SecurityUtils.getCurrentUserLogin().get());
        return ResponseEntity.ok().body(assertionsService.findById(id));
    }

    @GetMapping("/assertion/links")
    public void generateLinks(HttpServletResponse response) throws IOException, JSONException {
        final String fileName = dateFormat.format(new Date()) + "_orcid_permission_links.csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Type", "text/csv");
        response.setHeader("filename", fileName);
        String csvReport = orcidRecordService.generateLinks();
        response.getOutputStream().write(csvReport.getBytes());
        response.flushBuffer();
    }

    @PutMapping("/assertion")
    public ResponseEntity<Assertion> updateAssertion(@Valid @RequestBody Assertion assertion) throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to update assertion : {}", assertion);
        validateAssertion(assertion);
        Assertion existingAssertion = assertionsService.updateAssertion(assertion);

        return ResponseEntity.ok().body(existingAssertion);
    }

    @PostMapping("/assertion")
    public ResponseEntity<Assertion> createAssertion(@Valid @RequestBody Assertion assertion) throws BadRequestAlertException, URISyntaxException {
        LOG.debug("REST request to create assertion : {}", assertion);
        validateAssertion(assertion);
        assertion = assertionsService.createAssertion(assertion);

        return ResponseEntity.created(new URI("/api/assertion/" + assertion.getId()))
                .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, assertion.getId())).body(assertion);
    }

    @PostMapping("/assertion/upload")
    public ResponseEntity<String> uploadAssertions(@RequestParam("file") MultipartFile file) {
    	InputStream inputStream = null;
    	try {
			inputStream = file.getInputStream();
		} catch (IOException e) {
            LOG.warn("Error reading user upload", e);
            throw new RuntimeException(e);
		}
        AssertionsUpload upload = null;
		try {
			upload = assertionsCsvReader.readAssertionsUpload(inputStream);
		} catch (IOException e) {
            LOG.warn("Error reading user upload", e);
            throw new RuntimeException(e);
		}
		
		// add put codes for assertions that already exist
		updateIdsForExistingAssertions(upload.getAssertions());
		assertionsService.createOrUpdateAssertions(upload.getAssertions());

        return ResponseEntity.ok().body(upload.getErrors().toString());
    }

    private void updateIdsForExistingAssertions(List<Assertion> assertions) {
		assertions.forEach(a -> {
			Assertion existing = getExistingAssertion(a);
			if (existing != null) {
				a.setId(existing.getId());
			}
		});
	}

    @DeleteMapping("/assertion/{id}")
    public ResponseEntity<String> deleteAssertion(@PathVariable String id) throws BadRequestAlertException {
        assertionsService.deleteById(id);

        return ResponseEntity.ok().body("{\"id\":\"" + id + "\"}");
    }

	/**
	 * Returns owner id of an orcid record
	 *
	 * @param encryptedEmail
	 * @return ownerId of the orcid record
	 * @throws IOException
	 * @throws JSONException
	 */
    @GetMapping("/assertion/owner/{encryptedEmail}")
    public ResponseEntity<String> getOrcidRecordOwnerId(@PathVariable String encryptedEmail) throws IOException, JSONException {
    	String email = encryptUtil.decrypt(encryptedEmail);
    	Optional<OrcidRecord> record = orcidRecordService.findOneByEmail(email);
    	if (record.isPresent()) {
    		return ResponseEntity.ok().body(assertionsUserService.getLoggedInUserId());
    	} else {
    		return ResponseEntity.notFound().build();
    	}
    }

    /**
	 * Returns information about the ORCID record associated with a given user identified by encrypted email
	 *
	 * @param state encrypted email
	 * @return record information associated with the user identified by state
	 * @throws IOException
	 * @throws JSONException
	 */
    @GetMapping("/assertion/record/{state}")
    public ResponseEntity<OrcidRecord> getOrcidRecord(@PathVariable String state) throws IOException, JSONException {
    	String decryptState = encryptUtil.decrypt(state);
    	String[] stateTokens = decryptState.split("&&");
    	Optional<OrcidRecord> record = orcidRecordService.findOneByEmail(stateTokens[1]);
    	if (record.isPresent()) {
    		return ResponseEntity.ok().body(record.get());
    	} else {
    		return ResponseEntity.notFound().build();
    	}
    }

    @DeleteMapping("/assertion/orcid/{id}")
    public ResponseEntity<String> deleteAssertionFromOrcid(@PathVariable String id) throws JAXBException {
        Boolean deleted = assertionsService.deleteAssertionFromOrcid(id);
        JSONObject responseData = new JSONObject();
        responseData.put("deleted", deleted);

        if (!deleted) {
        	// fetch failure details
        	Assertion assertion = assertionsService.findById(id);
        	String errorJson = assertion.getOrcidError();
        	JSONObject obj = new JSONObject(errorJson);
    		int statusCode = (int) obj.get("statusCode");
    		String error = (String) obj.get("error");
    		responseData.put("statusCode", statusCode);
    		responseData.put("error", error);
        }

        return ResponseEntity.ok().body(responseData.toString());
    }
    
    @DeleteMapping("/assertion/delete/{salesforceId}")
    public ResponseEntity<String> deleteAssertionsForSalesforceId(@PathVariable String salesforceId) throws JAXBException {
        assertionsService.deleteAllBySalesforceId(salesforceId);
        JSONObject responseData = new JSONObject();
        responseData.put("deleted", true);
        return ResponseEntity.ok().body(responseData.toString());
    }

    @PostMapping("/id-token")
    public ResponseEntity<Void> storeIdToken(@RequestBody ObjectNode json) throws ParseException {
        String state = json.get("state").asText();
        String idToken = json.has("id_token") ? json.get("id_token").asText() : null;
        String salesForceId = json.has("salesforce_id") ? json.get("salesforce_id").asText() : null;
        Boolean denied = json.has("denied") ? json.get("denied").asBoolean() : false;
        String[] stateTokens = encryptUtil.decrypt(state).split("&&");
        String emailInStatus = stateTokens[1];

        if (!denied) {
            SignedJWT jwt = jwtUtil.getSignedJWT(idToken);
            String orcidIdInJWT = String.valueOf(jwt.getJWTClaimsSet().getClaim("sub"));

            if (!StringUtils.isBlank(emailInStatus) && !StringUtils.isBlank(orcidIdInJWT)) {
                orcidRecordService.storeIdToken(emailInStatus , idToken, orcidIdInJWT, salesForceId);
                try {
                	List<Assertion> assertions = assertionsService.findAssertionsByEmail(emailInStatus);
                	for(Assertion a:assertions) {
                		if(StringUtils.isBlank(a.getPutCode())) {
                			assertionsService.putAssertionToOrcid(a);
                		}
                		else if(a.isUpdated()) { 			
                			assertionsService.postAssertionToOrcid(a);
                		}
                	}
                	
                } catch (Exception ex) {
                	LOG.error("Error when posting the affiliations for user " + emailInStatus + " after granting permission.", ex);
                }
            } else {
                if (StringUtils.isBlank(emailInStatus)) {
                    LOG.warn("emailInStatus is empty in the state key: " + state);
                }

                if (StringUtils.isBlank(orcidIdInJWT)) {
                    LOG.warn("orcidIdInJWT is empty in the id token: " + idToken);
                }
            }
        } else {
            LOG.warn("User {} have denied access", emailInStatus);
            orcidRecordService.storeUserDeniedAccess(emailInStatus);
        }

        return ResponseEntity.ok().build();
    }

    @GetMapping(path = "/assertion/report")
    public void generateReport(HttpServletResponse response) throws IOException {
        final String fileName = dateFormat.format(new Date()) + "_orcid_report.csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Type", "text/csv");
        response.setHeader("filename", fileName);
        String csvReport = assertionsService.generateAssertionsReport();
        response.getOutputStream().write(csvReport.getBytes());
        response.flushBuffer();
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
        if (assertion.getDisambiguationSource() == null || StringUtils.isBlank(assertion.getDisambiguationSource())) {
           throw new BadRequestAlertException("disambiguation-source must not be null", "member", "disambiguationSource");
        }
        assertion.setUrl(validateUrl(assertion.getUrl()));
    }

    private String validateUrl(String url) {
        if (!StringUtils.isBlank(url)) {
            url = url.trim();
            boolean valid = false;
            try {
            	url = encodeUrl(url);
                valid = urlValidator.isValid(url);
            } catch (Exception e) {
            }

            if (!valid) {
                throw new BadRequestAlertException("Url is invalid", "member", "invalidUrl");
            }
        }
        return url;
    }

    private String encodeUrl(String urlString) throws MalformedURLException, URISyntaxException {
    	URL url = null;
    	try {
    		url = new URL(urlString);
    	} catch (MalformedURLException e) {
    		// try adding protocol, which could be missing
    		url = new URL("http://" + urlString);
    	}
        URI encoded = new URI(url.getProtocol(), url.getHost(), url.getPath(), null);
        return encoded.toASCIIString();
    }

	private Assertion getExistingAssertion(Assertion a) {
		List<Assertion> existing = assertionsService.getAssertionsBySalesforceId(a.getSalesforceId());
        for (Assertion existingAAssertion : existing) {
            // If the email is the same
            if (a.getEmail().equals(existingAAssertion.getEmail())) {
                // And is the same affiliation type
                if (a.getAffiliationSection().equals(existingAAssertion.getAffiliationSection())) {
                    // And the same department name
                    if ((StringUtils.isBlank(a.getDepartmentName()) && StringUtils.isBlank(existingAAssertion.getDepartmentName()))
                            || (a.getDepartmentName().equals(existingAAssertion.getDepartmentName()))) {
                        // And the same role title
                        if ((StringUtils.isBlank(a.getRoleTitle()) && StringUtils.isBlank(existingAAssertion.getRoleTitle()))
                                || (a.getRoleTitle().equals(existingAAssertion.getRoleTitle()))) {
                            // And the same org name
                            if (a.getOrgName().equals(existingAAssertion.getOrgName())) {
                                return existingAAssertion;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * {@code PUT /assertion/update/:salesforceId/:newSalesforceId} : Updates salesForceId for existing Assertions.
     *
     * @param salesforceId the salesforceId to the find the assertions to update.
     * @param newSalesforceId the new salesforceId to update.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/assertion/update/{salesforceId}/{newSalesforceId}")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> updateUserSalesforceOrAssertion(@PathVariable String salesforceId, @PathVariable String newSalesforceId) {
        LOG.debug("REST request to update Assertions by salesforce : {}", salesforceId);
        List<Assertion> assertionsBySalesforceId = assertionsService.getAssertionsBySalesforceId(salesforceId);
        for (Assertion assertion: assertionsBySalesforceId) {
            assertionsService.updateAssertionSalesforceId(assertion, newSalesforceId);
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, "assertion", salesforceId))
            .build();
    }

}
