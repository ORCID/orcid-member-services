package org.orcid.mp.assertion.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.codehaus.jettison.json.JSONException;


import org.codehaus.jettison.json.JSONObject;
import org.orcid.mp.assertion.client.MemberServiceClient;
import org.orcid.mp.assertion.client.UserServiceClient;
import org.orcid.mp.assertion.config.Constants;
import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.domain.AssertionStatus;
import org.orcid.mp.assertion.domain.OrcidRecord;
import org.orcid.mp.assertion.domain.User;
import org.orcid.mp.assertion.error.BadRequestAlertException;
import org.orcid.mp.assertion.error.RegistryDeleteFailureException;
import org.orcid.mp.assertion.pojo.AssertionDeletion;
import org.orcid.mp.assertion.pojo.NotificationRequest;
import org.orcid.mp.assertion.pojo.NotificationRequestInProgress;
import org.orcid.mp.assertion.security.EncryptUtil;
import org.orcid.mp.assertion.security.SecurityUtil;
import org.orcid.mp.assertion.service.AssertionService;
import org.orcid.mp.assertion.service.NotificationService;
import org.orcid.mp.assertion.service.OrcidRecordService;
import org.orcid.mp.assertion.util.AssertionUtils;
import org.orcid.mp.assertion.util.JWTUtil;
import org.orcid.mp.assertion.validation.OrcidUrlValidator;
import org.orcid.mp.assertion.validation.org.impl.GridOrgValidator;
import org.orcid.mp.assertion.validation.org.impl.RinggoldOrgValidator;
import org.orcid.mp.assertion.validation.org.impl.RorOrgValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.util.UriComponentsBuilder;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.*;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/assertions")
public class AssertionResource {

    private final Logger LOG = LoggerFactory.getLogger(AssertionResource.class);

    private final String GRID_SOURCE_ID = "GRID";

    @Autowired
    private AssertionService assertionService;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private JWTUtil jwtUtil;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private RinggoldOrgValidator ringgoldOrgValidator;

    @Autowired
    private GridOrgValidator gridOrgValidator;

    @Autowired
    private RorOrgValidator rorOrgValidator;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MemberServiceClient memberServiceClient;

    private final EmailValidator emailValidator = EmailValidator.getInstance(false);

    String[] urlValschemes = {"http", "https", "ftp"}; // DEFAULT schemes =
    // "http", "https",
    // "ftp"

    UrlValidator urlValidator = new OrcidUrlValidator(urlValschemes);

    @GetMapping
    public ResponseEntity<Page<Assertion>> getAssertions(Pageable pageable, @RequestParam MultiValueMap<String, String> queryParams, UriComponentsBuilder uriBuilder,
                                                         @RequestParam(required = false, name = "filter") String filter) throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch assertions from user {}", SecurityUtil.getCurrentUserLogin().get());
        Page<Assertion> affiliations = null;
        if (org.apache.commons.lang3.StringUtils.isBlank(filter)) {
            affiliations = assertionService.findByCurrentSalesforceId(pageable);
        } else {
            affiliations = assertionService.findBySalesforceId(pageable, filter);
        }
        return ResponseEntity.ok().body(affiliations);
    }

    @GetMapping("/{email}")
    public ResponseEntity<List<Assertion>> getAssertionsByEmail(@PathVariable String email) throws BadRequestAlertException, JSONException {
        LOG.debug("REST request to fetch assertions for email {}", email);
        List<Assertion> assertions = assertionService.findByEmail(email);
        return ResponseEntity.ok().body(assertions);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assertion> getAssertion(@PathVariable String id) {
        LOG.debug("REST request to fetch assertion {} from user {}", id, SecurityUtil.getCurrentUserLogin().get());
        Assertion assertion = assertionService.findById(id);
        if (permissionLinkRequired(assertion)) {
            assertionService.populatePermissionLink(assertion);
        }
        return ResponseEntity.ok().body(assertion);
    }

    @PostMapping("/permission-links")
    public ResponseEntity<Void> generatePermissionLinks() throws IOException, JSONException {
        String userLogin = SecurityUtil.getCurrentUserLogin().get();
        LOG.info("Permission links requested by {}", userLogin);
        assertionService.generatePermissionLinks();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/notification-request")
    public ResponseEntity<Void> sendNotifications(@RequestBody NotificationRequest notificationRequest) {
        User user = getLoggedInUser();
        memberServiceClient.updateMemberDefaultLanguage(user.getSalesforceId(), notificationRequest.getLanguage());
        notificationService.createSendNotificationsRequest(user.getEmail(), user.getSalesforceId());
        assertionService.markPendingAssertionsAsNotificationRequested(user.getSalesforceId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notification-request")
    public ResponseEntity<NotificationRequestInProgress> getNotificationRequestInProgress() {
        boolean notificationRequestInProgress = notificationService.requestInProgress(getLoggedInUser().getSalesforceId());
        return ResponseEntity.ok().body(new NotificationRequestInProgress(notificationRequestInProgress));
    }

    @PutMapping
    public ResponseEntity<Assertion> updateAssertion(@javax.validation.Valid @RequestBody Assertion assertion) throws BadRequestAlertException, JSONException {
        validateAssertion(assertion);
        Assertion existingAssertion = assertionService.updateAssertion(assertion, getLoggedInUser());
        LOG.info("{} updated assertion {}", SecurityUtil.getCurrentUserLogin().get(), assertion.getId());
        return ResponseEntity.ok().body(existingAssertion);
    }

    @PostMapping
    public ResponseEntity<Assertion> createAssertion(@javax.validation.Valid @RequestBody Assertion assertion) throws BadRequestAlertException, URISyntaxException {
        LOG.debug("REST request to create assertion : {}", assertion);
        validateAssertion(assertion);
        assertion = assertionService.createAssertion(assertion, getLoggedInUser());
        LOG.info("{} created assertion {}", SecurityUtil.getCurrentUserLogin().get(), assertion.getId());
        return ResponseEntity.created(new URI("/api/assertion/" + assertion.getId())).body(assertion);
    }

    @PostMapping("/upload")
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

    @DeleteMapping("/{id}")
    public ResponseEntity<AssertionDeletion> deleteAssertion(@PathVariable String id) throws BadRequestAlertException {
        try {
            assertionService.deleteById(id, getLoggedInUser());
            LOG.info("{} deleted assertion {}", SecurityUtil.getCurrentUserLogin().get(), id);
            return ResponseEntity.ok().body(new AssertionDeletion(true));
        } catch (RegistryDeleteFailureException e) {
            return ResponseEntity.ok().body(new AssertionDeletion(false));
        }
    }

    /**
     * Returns owner id of an orcid record
     *
     * @param encryptedEmail
     * @return ownerId of the orcid record
     * @throws IOException
     * @throws JSONException
     */
    @GetMapping("/owner/{encryptedEmail}")
    public ResponseEntity<String> getOrcidRecordOwnerId(@PathVariable String encryptedEmail) throws IOException, JSONException {
        String email = encryptUtil.decrypt(encryptedEmail);
        Optional<OrcidRecord> record = orcidRecordService.findByEmail(email);
        if (record.isPresent()) {
            return ResponseEntity.ok().body(getLoggedInUser().getId());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Returns information about the ORCID record associated with a given user
     * identified by encrypted email
     *
     * @param state encrypted email
     * @return record information associated with the user identified by state
     * @throws IOException
     * @throws JSONException
     */
    @GetMapping("/record/{state}")
    public ResponseEntity<OrcidRecord> getOrcidRecord(@PathVariable String state) throws IOException, JSONException {
        String decryptState = encryptUtil.decrypt(state);
        String[] stateTokens = decryptState.split("&&");
        Optional<OrcidRecord> optional = orcidRecordService.findByEmail(stateTokens[1]);
        if (optional.isPresent()) {
            OrcidRecord record = optional.get();
            if (org.apache.commons.lang3.StringUtils.isBlank(record.getToken(stateTokens[0], false))) {
                record.setOrcid(null);
            }
            return ResponseEntity.ok().body(record);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/id-token")
    public ResponseEntity<String> storeIdToken(@RequestBody ObjectNode json) throws ParseException, JAXBException, JSONException {
        String state = json.get("state").asText();
        String idToken = json.has("id_token") ? json.get("id_token").asText() : null;
        String salesforceId = json.has("salesforce_id") ? json.get("salesforce_id").asText() : null;
        Boolean denied = json.has("denied") && json.get("denied").asBoolean();
        String[] stateTokens = encryptUtil.decrypt(state).split("&&");
        String emailInStatus = stateTokens[1];
        if (salesforceId == null) {
            salesforceId = stateTokens[0];
        }
        JSONObject responseData = new JSONObject();

        if (!denied) {
            SignedJWT jwt = jwtUtil.getSignedJWT(idToken);
            String orcidIdInJWT = String.valueOf(jwt.getJWTClaimsSet().getClaim("sub"));
            // check it orcidid from jwt is the same as the one in record if
            // exists
            Optional<OrcidRecord> optional = orcidRecordService.findByEmail(stateTokens[1]);
            if (optional.isPresent()) {
                OrcidRecord record = optional.get();
                if (!org.apache.commons.lang3.StringUtils.isBlank(orcidIdInJWT) && !org.apache.commons.lang3.StringUtils.isBlank(record.getToken(stateTokens[0], false))
                        && !org.apache.commons.lang3.StringUtils.equals(record.getOrcid(), orcidIdInJWT)) {
                    responseData.put("isDifferentUser", true);
                    responseData.put("isSameUserThatAlreadyGranted", false);
                    return ResponseEntity.ok().body(responseData.toString());
                }
                // still need to store the token in case the used had denied
                // access before
                if (!org.apache.commons.lang3.StringUtils.isBlank(orcidIdInJWT) && !org.apache.commons.lang3.StringUtils.isBlank(record.getToken(stateTokens[0], false))
                        && org.apache.commons.lang3.StringUtils.equals(record.getOrcid(), orcidIdInJWT)) {
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

            if (!org.apache.commons.lang3.StringUtils.isBlank(emailInStatus) && !org.apache.commons.lang3.StringUtils.isBlank(orcidIdInJWT)) {
                orcidRecordService.storeIdToken(emailInStatus, idToken, orcidIdInJWT, salesforceId);
                assertionService.updateOrcidIdsForEmailAndSalesforceId(emailInStatus, salesforceId);
            } else {
                if (org.apache.commons.lang3.StringUtils.isBlank(emailInStatus)) {
                    LOG.warn("Not storing token for user {} - emailInStatus is empty in the state key: {}", emailInStatus, state);
                }

                if (org.apache.commons.lang3.StringUtils.isBlank(orcidIdInJWT)) {
                    LOG.warn("Not storing token for user {} - orcidIdInJWT is empty id token", emailInStatus);
                }
            }
        } else {
            LOG.info("User {} denied access", emailInStatus);
            orcidRecordService.storeUserDeniedAccess(emailInStatus, salesforceId);
        }
        return ResponseEntity.ok().body(responseData.toString());
    }

    @PostMapping(path = "/report")
    public ResponseEntity<Void> generateReport() throws IOException {
        String userLogin = SecurityUtil.getCurrentUserLogin().get();
        LOG.info("CSV report requested by {}", userLogin);
        assertionService.generateAssertionsReport();
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/csv")
    public ResponseEntity<Void> generateCsv() throws IOException {
        String userLogin = SecurityUtil.getCurrentUserLogin().get();
        LOG.info("CSV for editing requested by {}", userLogin);
        assertionService.generateAssertionsCSV();
        return ResponseEntity.ok().build();
    }

    private boolean permissionLinkRequired(Assertion assertion) {
        return AssertionStatus.PENDING.name().equals(assertion.getStatus()) || AssertionStatus.USER_REVOKED_ACCESS.name().equals(assertion.getStatus())
                || AssertionStatus.USER_DENIED_ACCESS.name().equals(assertion.getStatus()) || AssertionStatus.NOTIFICATION_SENT.name().equals(assertion.getStatus());
    }

    private void validateAssertion(Assertion assertion) {

        if (org.apache.commons.lang3.StringUtils.isBlank(assertion.getEmail())) {
            throw new IllegalArgumentException("email must not be null");
        }

        if (!emailValidator.isValid(assertion.getEmail())) {
            throw new BadRequestAlertException("Invalid email");
        }

        if (assertion.getAffiliationSection() == null) {
            throw new IllegalArgumentException("affiliation-section must not be null");
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(assertion.getOrgName())) {
            throw new IllegalArgumentException("org-name must not be null");
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(assertion.getOrgCountry())) {
            throw new IllegalArgumentException("org-country must not be null");
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(assertion.getOrgCity())) {
            throw new IllegalArgumentException("org-city must not be null");
        }

        if (org.apache.commons.lang3.StringUtils.isBlank(assertion.getDisambiguatedOrgId())) {
            throw new IllegalArgumentException("disambiguated-organization-identifier must not be null");
        }

        if (assertion.getDisambiguationSource() == null || org.apache.commons.lang3.StringUtils.isBlank(assertion.getDisambiguationSource())) {
            throw new BadRequestAlertException("disambiguation-source must not be null");
        }

        if (!validOrgId(assertion)) {
            throw new IllegalArgumentException("invalid org id");
        }

        if (assertionService.isDuplicate(assertion)) {
            throw new BadRequestAlertException("This assertion already exists");
        }

        // XXX this isn't validating
        if (org.apache.commons.lang3.StringUtils.equals(assertion.getDisambiguationSource(), GRID_SOURCE_ID)) {
            assertion.setDisambiguatedOrgId(AssertionUtils.stripGridURL(assertion.getDisambiguatedOrgId()));
        }

        assertion.setUrl(validateUrl(assertion.getUrl()));
    }

    private boolean validOrgId(Assertion assertion) {
        if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(assertion.getDisambiguationSource(), Constants.GRID_ORG_SOURCE)) {
            return gridOrgValidator.validId(assertion.getDisambiguatedOrgId());
        } else if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(assertion.getDisambiguationSource(), Constants.RINGGOLD_ORG_SOURCE)) {
            return ringgoldOrgValidator.validId(assertion.getDisambiguatedOrgId());
        } else if (org.apache.commons.lang3.StringUtils.equalsIgnoreCase(assertion.getDisambiguationSource(), Constants.ROR_ORG_SOURCE)) {
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
                throw new BadRequestAlertException("Url is invalid");
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

    private User getLoggedInUser() {
        String userLogin = SecurityUtil.getCurrentUserLogin().get();
        return userServiceClient.getUser(userLogin);
    }

}