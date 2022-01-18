package org.orcid.memberportal.service.assertion.web.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.codehaus.jettison.json.JSONException;
import org.json.JSONObject;
import org.orcid.memberportal.service.assertion.config.Constants;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.domain.utils.AssertionUtils;
import org.orcid.memberportal.service.assertion.domain.validation.OrcidUrlValidator;
import org.orcid.memberportal.service.assertion.domain.validation.org.impl.GridOrgValidator;
import org.orcid.memberportal.service.assertion.domain.validation.org.impl.RinggoldOrgValidator;
import org.orcid.memberportal.service.assertion.domain.validation.org.impl.RorOrgValidator;
import org.orcid.memberportal.service.assertion.security.AuthoritiesConstants;
import org.orcid.memberportal.service.assertion.security.EncryptUtil;
import org.orcid.memberportal.service.assertion.security.JWTUtil;
import org.orcid.memberportal.service.assertion.security.SecurityUtils;
import org.orcid.memberportal.service.assertion.services.AssertionService;
import org.orcid.memberportal.service.assertion.services.OrcidRecordService;
import org.orcid.memberportal.service.assertion.services.UserService;
import org.orcid.memberportal.service.assertion.web.rest.errors.BadRequestAlertException;
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

    private final String GRID_SOURCE_ID = "GRID";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Autowired
    private AssertionService assertionService;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserService assertionsUserService;

    @Autowired
    private RinggoldOrgValidator ringgoldOrgValidator;

    @Autowired
    private GridOrgValidator gridOrgValidator;

    @Autowired
    private RorOrgValidator rorOrgValidator;

    private EmailValidator emailValidator = EmailValidator.getInstance(false);

    String[] urlValschemes = { "http", "https", "ftp" }; // DEFAULT schemes =
    // "http", "https",
    // "ftp"

    UrlValidator urlValidator = new OrcidUrlValidator(urlValschemes);

    private SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY_MM_dd");

    @GetMapping("/assertions")
    public ResponseEntity<List<Assertion>> getAssertions(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder,
            @RequestParam(required = false, name = "filter") String filter) throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch assertions from user {}", SecurityUtils.getCurrentUserLogin().get());
        Page<Assertion> affiliations = null;
        if (StringUtils.isBlank(filter)) {
            affiliations = assertionService.findBySalesforceId(pageable);
        } else {
            affiliations = assertionService.findBySalesforceId(pageable, filter);
        }
        setPrettyStatus(affiliations);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(uriBuilder.queryParams(queryParams), affiliations);
        return ResponseEntity.ok().headers(headers).body(affiliations.getContent());
    }

    private void setPrettyStatus(Page<Assertion> affiliations) {
        affiliations.forEach(a -> { a.setPrettyStatus(AssertionStatus.valueOf(a.getStatus()).getValue()); });
    }

    @GetMapping("/assertions/{email}")
    public ResponseEntity<List<Assertion>> getAssertionsByEmail(@PathVariable String email) throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch assertions for email {}", email);
        List<Assertion> assertions = assertionService.findByEmail(email);
        return ResponseEntity.ok().body(assertions);
    }

    @GetMapping("/assertion/{id}")
    public ResponseEntity<Assertion> getAssertion(@PathVariable String id) throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch assertion {} from user {}", id, SecurityUtils.getCurrentUserLogin().get());
        return ResponseEntity.ok().body(assertionService.findById(id));
    }

    @GetMapping("/assertion/permission-links")
    public void generatePermissionLinks(HttpServletResponse response) throws IOException, JSONException {
        final String fileName = dateFormat.format(new Date()) + "_orcid_permission_links.csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Type", "text/csv");
        response.setHeader("filename", fileName);
        String csvReport = assertionService.generatePermissionLinks();
        response.getOutputStream().write(csvReport.getBytes());
        response.flushBuffer();
        LOG.info("{} generated assertions permission links", SecurityUtils.getCurrentUserLogin().get());
    }

    @PutMapping("/assertion")
    public ResponseEntity<Assertion> updateAssertion(@Valid @RequestBody Assertion assertion) throws BadRequestAlertException, JSONException {
        validateAssertion(assertion);
        Assertion existingAssertion = assertionService.updateAssertion(assertion, assertionsUserService.getLoggedInUser());
        LOG.info("{} updated assertion {}", SecurityUtils.getCurrentUserLogin().get(), assertion.getId());
        return ResponseEntity.ok().body(existingAssertion);
    }

    @PostMapping("/assertion")
    public ResponseEntity<Assertion> createAssertion(@Valid @RequestBody Assertion assertion) throws BadRequestAlertException, URISyntaxException {
        LOG.debug("REST request to create assertion : {}", assertion);
        validateAssertion(assertion);
        assertion = assertionService.createAssertion(assertion, assertionsUserService.getLoggedInUser());
        LOG.info("{} created assertion {}", SecurityUtils.getCurrentUserLogin().get(), assertion.getId());
        return ResponseEntity.created(new URI("/api/assertion/" + assertion.getId())).body(assertion);
    }

    @PostMapping("/assertion/upload")
    public ResponseEntity<Boolean> uploadAssertions(@RequestParam("file") MultipartFile file) {
        LOG.info("Uploading user csv upload for processing");
        try {
            assertionService.uploadAssertions(file);
            return ResponseEntity.ok().body(Boolean.TRUE);
        } catch (IOException e) {
            LOG.error("Error uploading user csv file", e);
            return ResponseEntity.ok().body(Boolean.FALSE); 
        }
    }

    @DeleteMapping("/assertion/{id}")
    public ResponseEntity<String> deleteAssertion(@PathVariable String id) throws BadRequestAlertException {
        assertionService.deleteById(id, assertionsUserService.getLoggedInUser());
        LOG.info("{} deleted assertion {}", SecurityUtils.getCurrentUserLogin().get(), id);
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
     * Returns information about the ORCID record associated with a given user
     * identified by encrypted email
     *
     * @param state
     *            encrypted email
     * @return record information associated with the user identified by state
     * @throws IOException
     * @throws JSONException
     */
    @GetMapping("/assertion/record/{state}")
    public ResponseEntity<OrcidRecord> getOrcidRecord(@PathVariable String state) throws IOException, JSONException {
        String decryptState = encryptUtil.decrypt(state);
        String[] stateTokens = decryptState.split("&&");
        Optional<OrcidRecord> optional = orcidRecordService.findOneByEmail(stateTokens[1]);
        if (optional.isPresent()) {
            OrcidRecord record = optional.get();
            if (StringUtils.isBlank(record.getToken(stateTokens[0]))) {
                record.setOrcid(null);
            }
            return ResponseEntity.ok().body(record);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/assertion/orcid/{id}")
    public ResponseEntity<String> deleteAssertionFromOrcid(@PathVariable String id) throws JAXBException {
        Boolean deleted = assertionService.deleteAssertionFromOrcidRegistry(id, assertionsUserService.getLoggedInUser());
        JSONObject responseData = new JSONObject();
        responseData.put("deleted", deleted);

        if (!deleted) {
            // fetch failure details
            Assertion assertion = assertionService.findById(id);
            String errorJson = assertion.getOrcidError();
            JSONObject obj = new JSONObject(errorJson);
            int statusCode = (int) obj.get("statusCode");
            String error = (String) obj.get("error");
            responseData.put("statusCode", statusCode);
            responseData.put("error", error);
            LOG.info("{} failed to delete assertion {} from orcid", SecurityUtils.getCurrentUserLogin().get(), id);
        } else {
            LOG.info("{} deleted assertion {} from orcid", SecurityUtils.getCurrentUserLogin().get(), id);
        }

        return ResponseEntity.ok().body(responseData.toString());
    }

    @DeleteMapping("/assertion/delete/{salesforceId}")
    public ResponseEntity<String> deleteAssertionsForSalesforceId(@PathVariable String salesforceId) throws JAXBException {
        assertionService.deleteAllBySalesforceId(salesforceId);
        JSONObject responseData = new JSONObject();
        responseData.put("deleted", true);
        return ResponseEntity.ok().body(responseData.toString());
    }

    @PostMapping("/id-token")
    public ResponseEntity<String> storeIdToken(@RequestBody ObjectNode json) throws ParseException, JAXBException {
        String state = json.get("state").asText();
        String idToken = json.has("id_token") ? json.get("id_token").asText() : null;
        String salesForceId = json.has("salesforce_id") ? json.get("salesforce_id").asText() : null;
        Boolean denied = json.has("denied") ? json.get("denied").asBoolean() : false;
        String[] stateTokens = encryptUtil.decrypt(state).split("&&");
        String emailInStatus = stateTokens[1];
        if (salesForceId == null) {
            salesForceId = stateTokens[0];
        }
        JSONObject responseData = new JSONObject();

        if (!denied) {
            SignedJWT jwt = jwtUtil.getSignedJWT(idToken);
            String orcidIdInJWT = String.valueOf(jwt.getJWTClaimsSet().getClaim("sub"));
            // check it orcidid from jwt is the same as the one in record if
            // exists
            Optional<OrcidRecord> optional = orcidRecordService.findOneByEmail(stateTokens[1]);
            if (optional.isPresent()) {
                OrcidRecord record = optional.get();
                if (!StringUtils.isBlank(orcidIdInJWT) && !StringUtils.isBlank(record.getToken(stateTokens[0])) && !StringUtils.equals(record.getOrcid(), orcidIdInJWT)) {
                    responseData.put("isDifferentUser", true);
                    responseData.put("isSameUserThatAlreadyGranted", false);
                    return ResponseEntity.ok().body(responseData.toString());
                }
                // still need to store the token in case the used had denied
                // access before
                if (!StringUtils.isBlank(orcidIdInJWT) && !StringUtils.isBlank(record.getToken(stateTokens[0])) && StringUtils.equals(record.getOrcid(), orcidIdInJWT)) {
                    responseData.put("isDifferentUser", false);
                    responseData.put("isSameUserThatAlreadyGranted", true);
                } else {
                    responseData.put("isDifferentUser", false);
                    responseData.put("isSameUserThatAlreadyGranted", false);
                }
            } else {
                responseData.put("isDifferentUser", false);
                responseData.put("isSameUserThatAlreadyGranted", false);
            }

            if (!StringUtils.isBlank(emailInStatus) && !StringUtils.isBlank(orcidIdInJWT)) {
                orcidRecordService.storeIdToken(emailInStatus, idToken, orcidIdInJWT, salesForceId);
                assertionService.updateOrcidIdsForEmail(emailInStatus);
            } else {
                if (StringUtils.isBlank(emailInStatus)) {
                    LOG.warn("Not storing token for user {} - emailInStatus is empty in the state key: {}", emailInStatus, state);
                }

                if (StringUtils.isBlank(orcidIdInJWT)) {
                    LOG.warn("Not storing token for user {} - orcidIdInJWT is empty id token", emailInStatus);
                }
            }
        } else {
            LOG.warn("User {} have denied access", emailInStatus);
            orcidRecordService.storeUserDeniedAccess(emailInStatus, salesForceId);
        }
        return ResponseEntity.ok().body(responseData.toString());
    }

    @GetMapping(path = "/assertion/report")
    public void generateReport(HttpServletResponse response) throws IOException {
        final String fileName = dateFormat.format(new Date()) + "_orcid_report.csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Type", "text/csv");
        response.setHeader("filename", fileName);
        String csvReport = assertionService.generateAssertionsReport();
        response.getOutputStream().write(csvReport.getBytes());
        response.flushBuffer();
        LOG.info("{} generated assertions CSV report", SecurityUtils.getCurrentUserLogin().get());
    }

    @GetMapping(path = "/assertion/csv")
    public void generateCsv(HttpServletResponse response) throws IOException {
        final String fileName = dateFormat.format(new Date()) + "_affiliations.csv";
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setHeader("Content-Type", "text/csv");
        response.setHeader("filename", fileName);
        String csvReport = assertionService.generateAssertionsCSV();
        response.getOutputStream().write(csvReport.getBytes());
        response.flushBuffer();
        LOG.info("{} generated assertions CSV for editing", SecurityUtils.getCurrentUserLogin().get());
    }

    private void validateAssertion(Assertion assertion) {

        if (StringUtils.isBlank(assertion.getEmail())) {
            throw new IllegalArgumentException("email must not be null");
        }

        if (!emailValidator.isValid(assertion.getEmail())) {
            throw new BadRequestAlertException("Invalid email", "email", "email.string");
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

        if (!validOrgId(assertion)) {
            throw new IllegalArgumentException("invalid org id");
        }

        if (assertionService.isDuplicate(assertion)) {
            throw new BadRequestAlertException("This assertion already exists", "assertion", "assertion.validation.duplicate.string");
        }

        // XXX this isn't validating
        if (StringUtils.equals(assertion.getDisambiguationSource(), GRID_SOURCE_ID)) {
            assertion.setDisambiguatedOrgId(AssertionUtils.stripGridURL(assertion.getDisambiguatedOrgId()));
        }

        assertion.setUrl(validateUrl(assertion.getUrl()));
    }

    private boolean validOrgId(Assertion assertion) {
        if (StringUtils.equalsIgnoreCase(assertion.getDisambiguationSource(), Constants.GRID_ORG_SOURCE)) {
            return gridOrgValidator.validId(assertion.getDisambiguatedOrgId());
        } else if (StringUtils.equalsIgnoreCase(assertion.getDisambiguationSource(), Constants.RINGGOLD_ORG_SOURCE)) {
            return ringgoldOrgValidator.validId(assertion.getDisambiguatedOrgId());
        } else if (StringUtils.equalsIgnoreCase(assertion.getDisambiguationSource(), Constants.ROR_ORG_SOURCE)) {
            return rorOrgValidator.validId(assertion.getDisambiguatedOrgId());
        }
        return false;
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
                throw new BadRequestAlertException("Url is invalid", "assertion", "invalidUrl.string");
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

    /**
     * {@code PUT /assertion/update/:salesforceId/:newSalesforceId} : Updates
     * salesForceId for existing Assertions.
     *
     * @param salesforceId
     *            the salesforceId to the find the assertions to update.
     * @param newSalesforceId
     *            the new salesforceId to update.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/assertion/update/{salesforceId}/{newSalesforceId}")
    @PreAuthorize("hasRole(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> updateUserSalesforceOrAssertion(@PathVariable String salesforceId, @PathVariable String newSalesforceId) {
        LOG.debug("REST request to update Assertions by salesforce : {}", salesforceId);
        List<Assertion> assertionsBySalesforceId = assertionService.getAssertionsBySalesforceId(salesforceId);
        for (Assertion assertion : assertionsBySalesforceId) {
            assertionService.updateAssertionSalesforceId(assertion, newSalesforceId);
        }
        return ResponseEntity.ok().headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, "assertion", salesforceId)).build();
    }

}
