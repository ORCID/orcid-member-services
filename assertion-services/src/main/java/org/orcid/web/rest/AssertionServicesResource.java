package org.orcid.web.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.InternalServerErrorException;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.codehaus.jettison.json.JSONException;
import org.orcid.domain.Assertion;
import org.orcid.domain.validation.OrcidUrlValidator;
import org.orcid.security.EncryptUtil;
import org.orcid.security.JWTUtil;
import org.orcid.security.SecurityUtils;
import org.orcid.service.AssertionsService;
import org.orcid.service.OrcidRecordService;
import org.orcid.service.assertions.upload.AssertionsUpload;
import org.orcid.service.assertions.upload.impl.AssertionsCsvReader;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.SignedJWT;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;

@RestController
@RequestMapping("/api")
public class AssertionServicesResource {
    
	private static final Logger LOG = LoggerFactory.getLogger(AssertionServicesResource.class);

    private static final String ENTITY_NAME = "affiliation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private AssertionsService assertionsService;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private JWTUtil jwtUtil;
    
    @Autowired
    private AssertionsCsvReader assertionsCsvReader;
    
    String[] urlValschemes = { "http", "https", "ftp" }; // DEFAULT schemes =
    // "http", "https",
    // "ftp"

    UrlValidator urlValidator = new OrcidUrlValidator(urlValschemes);
    
    @GetMapping("/assertions")
    public ResponseEntity<List<Assertion>> getAssertions(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder)
            throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch assertions from user {}", SecurityUtils.getCurrentUserLogin().get());

        Page<Assertion> affiliations = assertionsService.findBySalesforceId(pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), affiliations);
        return ResponseEntity.ok().headers(headers).body(affiliations.getContent());
    }

    @GetMapping("/assertion/{id}")
    public ResponseEntity<Assertion> getAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {        
        LOG.debug("REST request to fetch assertion {} from user {}", id, SecurityUtils.getCurrentUserLogin().get());
        return ResponseEntity.ok().body(assertionsService.findById(id));
    }
    
    @GetMapping("/assertion/links")
    public void generateLinks(HttpServletResponse response) throws IOException, JSONException {
        String loggedInUserId = SecurityUtils.getCurrentUserLogin().get();
        final String fileName = loggedInUserId + '_' +  System.currentTimeMillis() + "_report.csv";        
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
			LOG.warn("Error reading upload file", e);
			return ResponseEntity.badRequest().build();
		}
        AssertionsUpload upload = null;
		try {
			upload = assertionsCsvReader.readAssertionsUpload(inputStream);
		} catch (IOException e) {
			return ResponseEntity.badRequest().build();
		}
		
		// add put codes for assertions that already exist
		updateIdsForExistingAssertions(upload.getAssertions());
		assertionsService.createOrUpdateAssertions(upload.getAssertions());
        
        if (upload.getErrors().length() > 0) {
        	return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(upload.getErrors().toString());
        } else {
        	return ResponseEntity.ok().build();
        }
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

    @DeleteMapping("/assertion/orcid/{id}")
    public ResponseEntity<String> deleteAssertionFromOrcid(@PathVariable String id) throws JAXBException {
        Boolean deleted = assertionsService.deleteAssertionFromOrcid(id);
        return ResponseEntity.ok().body("{\"deleted\":\"" + deleted + "\"}");
    }
    
    @PostMapping("/id-token")
    public ResponseEntity<Void> storeIdToken(@RequestBody ObjectNode json) throws ParseException {
        String state = json.get("state").asText();
        String idToken = json.has("id_token") ? json.get("id_token").asText() : null;
        Boolean denied = json.has("denied") ? json.get("denied").asBoolean() : false;
        String emailInStatus = encryptUtil.decrypt(state);

        if (!denied) {
            SignedJWT jwt = jwtUtil.getSignedJWT(idToken);
            String orcidIdInJWT = String.valueOf(jwt.getJWTClaimsSet().getClaim("sub"));

            if (!StringUtils.isBlank(emailInStatus) && !StringUtils.isBlank(orcidIdInJWT)) {
                orcidRecordService.storeIdToken(emailInStatus, idToken, orcidIdInJWT);
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
        final String userLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new InternalServerErrorException("Current user login not found"));
        final String fileName = userLogin + '_' +  System.currentTimeMillis() + "_report.csv";        
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
        if (StringUtils.isBlank(assertion.getDisambiguationSource())) {
            throw new IllegalArgumentException("disambiguation-source must not be null");
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
            	throw new IllegalArgumentException("url is invalid");
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
		List<Assertion> existing = assertionsService.findAllByOwnerId();
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
	
}
