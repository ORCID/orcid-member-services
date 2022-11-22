package org.orcid.memberportal.service.assertion.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.config.Constants;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.OrcidToken;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.domain.validation.org.impl.GridOrgValidator;
import org.orcid.memberportal.service.assertion.domain.validation.org.impl.RinggoldOrgValidator;
import org.orcid.memberportal.service.assertion.domain.validation.org.impl.RorOrgValidator;
import org.orcid.memberportal.service.assertion.security.EncryptUtil;
import org.orcid.memberportal.service.assertion.security.JWTUtil;
import org.orcid.memberportal.service.assertion.security.MockSecurityContext;
import org.orcid.memberportal.service.assertion.services.AssertionService;
import org.orcid.memberportal.service.assertion.services.NotificationService;
import org.orcid.memberportal.service.assertion.services.OrcidRecordService;
import org.orcid.memberportal.service.assertion.services.UserService;
import org.orcid.memberportal.service.assertion.web.rest.errors.BadRequestAlertException;
import org.orcid.memberportal.service.assertion.web.rest.vm.NotificationRequestInProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class AssertionResourceTest {

    private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

    @Mock
    private AssertionService assertionService;

    @Mock
    private OrcidRecordService orcidRecordService;

    @Mock
    private EncryptUtil encryptUtil;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private RorOrgValidator rorOrgValidator;
    
    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService assertionsUserService;

    @Mock
    private RinggoldOrgValidator ringgoldOrgValidator;

    @Mock
    private GridOrgValidator gridOrgValidator;
    
    @InjectMocks
    private AssertionResource assertionResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SecurityContextHolder.setContext(new MockSecurityContext("user"));
        Mockito.when(rorOrgValidator.validId(Mockito.anyString())).thenReturn(true);
        Mockito.when(gridOrgValidator.validId(Mockito.anyString())).thenReturn(true);
        Mockito.when(ringgoldOrgValidator.validId(Mockito.anyString())).thenReturn(true);
        Mockito.when(assertionsUserService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn(DEFAULT_SALESFORCE_ID);
    }
    
    @Test
    void testGetAssertionOfPendingStatus() {
        Assertion pendingAssertion = new Assertion();
        pendingAssertion.setStatus(AssertionStatus.PENDING.name());
        Mockito.when(assertionService.findById(Mockito.eq("test"))).thenReturn(pendingAssertion);
        Mockito.doNothing().when(assertionService).populatePermissionLink(Mockito.any(Assertion.class));
        assertionResource.getAssertion("test");
        Mockito.verify(assertionService).findById(Mockito.eq("test"));
        Mockito.verify(assertionService).populatePermissionLink(Mockito.any(Assertion.class));
    }
    
    @Test
    void testGetAssertionOfNotificationSentStatus() {
        Assertion pendingAssertion = new Assertion();
        pendingAssertion.setStatus(AssertionStatus.NOTIFICATION_SENT.name());
        Mockito.when(assertionService.findById(Mockito.eq("test"))).thenReturn(pendingAssertion);
        Mockito.doNothing().when(assertionService).populatePermissionLink(Mockito.any(Assertion.class));
        assertionResource.getAssertion("test");
        Mockito.verify(assertionService).findById(Mockito.eq("test"));
        Mockito.verify(assertionService).populatePermissionLink(Mockito.any(Assertion.class));
    }
    
    @Test
    void testGetAssertionOfDeniedAccessStatus() {
        Assertion pendingAssertion = new Assertion();
        pendingAssertion.setStatus(AssertionStatus.USER_DENIED_ACCESS.name());
        Mockito.when(assertionService.findById(Mockito.eq("test"))).thenReturn(pendingAssertion);
        Mockito.doNothing().when(assertionService).populatePermissionLink(Mockito.any(Assertion.class));
        assertionResource.getAssertion("test");
        Mockito.verify(assertionService).findById(Mockito.eq("test"));
        Mockito.verify(assertionService).populatePermissionLink(Mockito.any(Assertion.class));
    }
    
    @Test
    void testGetAssertionOfRevokedAccessStatus() {
        Assertion pendingAssertion = new Assertion();
        pendingAssertion.setStatus(AssertionStatus.USER_REVOKED_ACCESS.name());
        Mockito.when(assertionService.findById(Mockito.eq("test"))).thenReturn(pendingAssertion);
        Mockito.doNothing().when(assertionService).populatePermissionLink(Mockito.any(Assertion.class));
        assertionResource.getAssertion("test");
        Mockito.verify(assertionService).findById(Mockito.eq("test"));
        Mockito.verify(assertionService).populatePermissionLink(Mockito.any(Assertion.class));
    }
    
    @Test
    void testGetAssertionOfInOrcidStatus() {
        Assertion pendingAssertion = new Assertion();
        pendingAssertion.setStatus(AssertionStatus.IN_ORCID.name());
        Mockito.when(assertionService.findById(Mockito.eq("test"))).thenReturn(pendingAssertion);
        assertionResource.getAssertion("test");
        Mockito.verify(assertionService).findById(Mockito.eq("test"));
        Mockito.verify(assertionService, Mockito.never()).populatePermissionLink(Mockito.any(Assertion.class));
    }
    
    @Test
    void testSendNotifications() {
        Mockito.when(assertionsUserService.getLoggedInUser()).thenReturn(getUser());
        Mockito.doNothing().when(notificationService).createSendNotificationsRequest(Mockito.eq("owner@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID));
        Mockito.doNothing().when(assertionService).markPendingAssertionsAsNotificationRequested(Mockito.eq("salesforce"));
        assertionResource.sendNotifications();
        Mockito.verify(notificationService).createSendNotificationsRequest(Mockito.eq("owner@orcid.org"), Mockito.eq(DEFAULT_SALESFORCE_ID));
        Mockito.verify(assertionService).markPendingAssertionsAsNotificationRequested(Mockito.eq(DEFAULT_SALESFORCE_ID));
    }
    
    @Test
    void testGetNotificationRequestInProgress_inProgressIsTrue() {
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn(DEFAULT_SALESFORCE_ID);
        Mockito.when(notificationService.requestInProgress(Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(true);
        
        ResponseEntity<NotificationRequestInProgress> response = assertionResource.getNotificationRequestInProgress();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getInProgress());
    }
    
    @Test
    void testGetNotificationRequestInProgress_inProgressIsFalse() {
        Mockito.when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn(DEFAULT_SALESFORCE_ID);
        Mockito.when(notificationService.requestInProgress(Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(false);
        
        ResponseEntity<NotificationRequestInProgress> response = assertionResource.getNotificationRequestInProgress();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().getInProgress());
    }

    @Test
    void testGetOrcidRecord() throws IOException, org.codehaus.jettison.json.JSONException {
        String email = "email@email.com";
        String encrypted = encryptUtil.encrypt(DEFAULT_SALESFORCE_ID + "&&" + email);

        OrcidRecord record = new OrcidRecord();
        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken");
        tokens.add(newToken);
        record.setTokens(tokens);

        Mockito.when(encryptUtil.decrypt(Mockito.eq(encrypted))).thenReturn(DEFAULT_SALESFORCE_ID + "&&" + email);
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq(email))).thenReturn(Optional.of(record));

        ResponseEntity<OrcidRecord> response = assertionResource.getOrcidRecord(encrypted);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());

        Mockito.verify(encryptUtil, Mockito.times(1)).decrypt(Mockito.eq(encrypted));
        Mockito.verify(orcidRecordService, Mockito.times(1)).findOneByEmail(Mockito.eq(email));

        String emailOther = "nope@email.com";
        String encryptedOther = encryptUtil.encrypt(DEFAULT_SALESFORCE_ID + "&&" + emailOther);

        Mockito.when(encryptUtil.decrypt(Mockito.eq(encryptedOther))).thenReturn(DEFAULT_SALESFORCE_ID + "&&" + emailOther);
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq(emailOther))).thenReturn(Optional.empty());

        response = assertionResource.getOrcidRecord(encryptedOther);
        assertTrue(response.getStatusCode().is4xxClientError());
        assertNull(response.getBody());

    }

    @Test
    void testGenerateCsv() throws IOException {
        Mockito.doNothing().when(assertionService).generateAssertionsCSV();
        assertionResource.generateCsv();
        Mockito.verify(assertionService).generateAssertionsCSV();
    }

    @Test
    void testGenerateReport() throws IOException {
        Mockito.doNothing().when(assertionService).generateAssertionsReport();
        assertionResource.generateReport();
        Mockito.verify(assertionService).generateAssertionsReport();
    }

    @Test
    void testGenerateLinks() throws Exception {
        Mockito.doNothing().when(assertionService).generatePermissionLinks();
        assertionResource.generatePermissionLinks();
        Mockito.verify(assertionService).generatePermissionLinks();
    }

    @Test
    void testCreateAssertion() throws BadRequestAlertException, URISyntaxException {
        Assertion creatingAssertion = getAssertion("test-create-assertion@orcid.org");
        Assertion createdAssertion = getAssertion("test-create-assertion@orcid.org");
        createdAssertion.setId("some-id-because-this-assertion-exists-already");
        Mockito.when(assertionService.isDuplicate(Mockito.any(Assertion.class), Mockito.anyString())).thenReturn(false);
        Mockito.when(assertionService.createAssertion(Mockito.any(Assertion.class), Mockito.any(AssertionServiceUser.class))).thenReturn(createdAssertion);
        ResponseEntity<Assertion> response = assertionResource.createAssertion(creatingAssertion);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Mockito.verify(assertionService, Mockito.times(1)).createAssertion(Mockito.any(Assertion.class), Mockito.any(AssertionServiceUser.class));
        Mockito.verify(assertionService, Mockito.times(1)).isDuplicate(Mockito.any(Assertion.class), Mockito.anyString());
    }

    @Test
    void testCreateAssertion_verifyOrgsValidated() throws BadRequestAlertException, URISyntaxException {
        Assertion creatingAssertion = getAssertion("test-create-assertion@orcid.org");
        creatingAssertion.setDisambiguationSource(Constants.GRID_ORG_SOURCE);
        creatingAssertion.setDisambiguatedOrgId("something");
        Assertion createdAssertion = getAssertion("test-create-assertion@orcid.org");
        createdAssertion.setId("some-id-because-this-assertion-exists-already");
        Mockito.when(assertionService.isDuplicate(Mockito.any(Assertion.class), Mockito.anyString())).thenReturn(false);
        Mockito.when(assertionService.createAssertion(Mockito.any(Assertion.class), Mockito.any(AssertionServiceUser.class))).thenReturn(createdAssertion);
        ResponseEntity<Assertion> response = assertionResource.createAssertion(creatingAssertion);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Mockito.verify(gridOrgValidator, Mockito.times(1)).validId(Mockito.eq("something"));

        creatingAssertion.setDisambiguationSource(Constants.RINGGOLD_ORG_SOURCE);
        response = assertionResource.createAssertion(creatingAssertion);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Mockito.verify(ringgoldOrgValidator, Mockito.times(1)).validId(Mockito.eq("something"));

        creatingAssertion.setDisambiguationSource(Constants.ROR_ORG_SOURCE);
        response = assertionResource.createAssertion(creatingAssertion);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Mockito.verify(gridOrgValidator, Mockito.times(1)).validId(Mockito.eq("something"));
    }

    @Test
    void testCreateAssertionInvalidEmail() throws BadRequestAlertException, URISyntaxException {
        Assertion creatingAssertion = getAssertion("test-create-assertion@orcid");
        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            assertionResource.createAssertion(creatingAssertion);
        });
    }

    @Test
    void testUpdateAssertion() throws BadRequestAlertException, URISyntaxException, org.codehaus.jettison.json.JSONException {
        Assertion assertion = getAssertion("test-update-assertion@orcid.org");
        assertion.setId("some-id-because-this-assertion-exists-already");
        Mockito.when(assertionService.isDuplicate(Mockito.any(Assertion.class), Mockito.anyString())).thenReturn(false);
        Mockito.when(assertionService.updateAssertion(Mockito.any(Assertion.class), Mockito.any(AssertionServiceUser.class))).thenReturn(assertion);
        ResponseEntity<Assertion> response = assertionResource.updateAssertion(assertion);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Mockito.verify(assertionService, Mockito.times(1)).updateAssertion(Mockito.any(Assertion.class), Mockito.any(AssertionServiceUser.class));
        Mockito.verify(assertionService, Mockito.times(1)).isDuplicate(Mockito.any(Assertion.class), Mockito.anyString());
    }

    @Test
    void testUpdateAssertionInvalidEmail() throws BadRequestAlertException, URISyntaxException {
        Assertion updatingAssertion = getAssertion("test-create-assertion@orcid");
        updatingAssertion.setId("some-id-because-this-assertion-exists-already");
        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            assertionResource.updateAssertion(updatingAssertion);
        });
    }

    @Test
    void testCreateDuplicateAssertion() throws BadRequestAlertException, URISyntaxException {
        Assertion creatingAssertion = getAssertion("test-create-assertion@orcid.org");
        Mockito.when(assertionService.isDuplicate(Mockito.any(Assertion.class), Mockito.anyString())).thenReturn(true);

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            assertionResource.createAssertion(creatingAssertion);
        });

        Mockito.verify(assertionService, Mockito.never()).createAssertion(Mockito.any(Assertion.class), Mockito.any(AssertionServiceUser.class));
        Mockito.verify(assertionService, Mockito.times(1)).isDuplicate(Mockito.any(Assertion.class), Mockito.anyString());
    }

    @Test
    void testUpdateDuplicateAssertion() throws BadRequestAlertException, URISyntaxException {
        Assertion assertion = getAssertion("test-update-assertion@orcid.org");
        Mockito.when(assertionService.isDuplicate(Mockito.any(Assertion.class), Mockito.anyString())).thenReturn(true);

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            assertionResource.updateAssertion(assertion);
        });

        Mockito.verify(assertionService, Mockito.never()).updateAssertion(Mockito.any(Assertion.class), Mockito.any(AssertionServiceUser.class));
        Mockito.verify(assertionService, Mockito.times(1)).isDuplicate(Mockito.any(Assertion.class), Mockito.anyString());
    }

    @Test
    void testUploadAssertions() throws IOException {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.doNothing().when(assertionService).uploadAssertions(Mockito.any());
        ResponseEntity<Boolean> success = assertionResource.uploadAssertions(file);
        assertEquals(Boolean.TRUE, success.getBody());
        Mockito.verify(assertionService, Mockito.times(1)).uploadAssertions(Mockito.any());
    }

    @Test
    void testUploadAssertionsErrorThrown() throws IOException {
        MultipartFile file = Mockito.mock(MultipartFile.class);
        Mockito.doThrow(new IOException()).when(assertionService).uploadAssertions(Mockito.any());
        ResponseEntity<Boolean> success = assertionResource.uploadAssertions(file);
        assertEquals(Boolean.FALSE, success.getBody());
        Mockito.verify(assertionService, Mockito.times(1)).uploadAssertions(Mockito.any());
    }

    @Test
    void testStoreIdToken() throws ParseException, JAXBException, JOSEException {
        String email = "email@orcid.org";
        String orcid = "1234-1234-1234-1234";
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq(email))).thenReturn(Optional.of(getOrcidRecord(email)));
        Mockito.when(encryptUtil.decrypt(Mockito.eq("ermmmm....&&" + email))).thenReturn("ermmmm....&&" + email);
        Mockito.when(jwtUtil.getSignedJWT(Mockito.anyString())).thenReturn(getDummySignedJWT(orcid));

        assertionResource.storeIdToken(getObjectNode(email));

        Mockito.verify(orcidRecordService, Mockito.times(1)).storeIdToken(Mockito.eq(email), Mockito.anyString(), Mockito.eq(orcid), Mockito.anyString());
        Mockito.verify(assertionService, Mockito.never()).postAssertionToOrcid(Mockito.any(Assertion.class));
        Mockito.verify(assertionService, Mockito.never()).putAssertionInOrcid(Mockito.any(Assertion.class));
        Mockito.verify(assertionService, Mockito.never()).updateAssertion(Mockito.any(Assertion.class), Mockito.any(AssertionServiceUser.class));
        Mockito.verify(assertionService).updateOrcidIdsForEmailAndSalesforceId(Mockito.eq(email), Mockito.eq(DEFAULT_SALESFORCE_ID));
    }

    @Test
    void testGetAssertions() throws BadRequestAlertException, org.codehaus.jettison.json.JSONException {
        Mockito.when(assertionService.findByCurrentSalesforceId(Mockito.any(Pageable.class))).thenReturn(getMockPage());
        ResponseEntity<List<Assertion>> page = assertionResource.getAssertions(Mockito.mock(Pageable.class), new HttpHeaders(), UriComponentsBuilder.newInstance(), "");
        assertNotNull(page.getBody());
    }

    private Page<Assertion> getMockPage() {
        Assertion assertion1 = getAssertion("some-email@orcid.org");
        assertion1.setStatus(AssertionStatus.PENDING.name());

        Assertion assertion2 = getAssertion("some-email@orcid.org");
        assertion2.setStatus(AssertionStatus.IN_ORCID.name());

        Assertion assertion3 = getAssertion("some-email@orcid.org");
        assertion3.setStatus(AssertionStatus.PENDING_RETRY.name());

        Assertion assertion4 = getAssertion("some-email@orcid.org");
        assertion4.setStatus(AssertionStatus.ERROR_ADDING_TO_ORCID.name());

        Assertion assertion5 = getAssertion("some-email@orcid.org");
        assertion5.setStatus(AssertionStatus.ERROR_UPDATING_TO_ORCID.name());

        Page<Assertion> page = new PageImpl<>(Arrays.asList(assertion1, assertion2, assertion3, assertion4, assertion5));
        return page;
    }

    private SignedJWT getDummySignedJWT(String orcid) throws JOSEException {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", orcid);
        Date now = new Date();
        JWTClaimsSet.Builder claimsSet = new JWTClaimsSet.Builder();
        claimsSet.issueTime(now);
        claimsSet.expirationTime(new Date(now.getTime() + (1000)));
        claimsSet.notBeforeTime(now);
        claimsSet.claim("something", "something else");
        claims.entrySet().forEach((claim) -> claimsSet.claim(claim.getKey(), claim.getValue()));
        JWSSigner signer = new MACSigner("something very secret indeed, really seriously secret.");
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet.build());
        signedJWT.sign(signer);
        return signedJWT;
    }

    private ObjectNode getObjectNode(String email) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.set("state", mapper.convertValue("ermmmm....&&" + email, JsonNode.class));
        node.set("id_token", mapper.convertValue("errrr....", JsonNode.class));
        node.set("salesforce_id", mapper.convertValue(DEFAULT_SALESFORCE_ID, JsonNode.class));
        node.set("denied", mapper.convertValue(false, JsonNode.class));
        return node;
    }

    private OrcidRecord getOrcidRecord(String email) {
        OrcidRecord record = new OrcidRecord();
        record.setEmail(email);
        return record;
    }

    private Assertion getAssertion(String email) {
        Assertion assertion = new Assertion();
        assertion.setAffiliationSection(AffiliationSection.DISTINCTION);
        assertion.setOrgName("org");
        assertion.setOrgCountry("US");
        assertion.setOrgCity("city");
        assertion.setDisambiguatedOrgId("something");
        assertion.setDisambiguationSource(Constants.RINGGOLD_ORG_SOURCE);
        assertion.setEmail(email);
        return assertion;
    }

    private AssertionServiceUser getUser() {
        AssertionServiceUser user = new AssertionServiceUser();
        user.setId("owner");
        user.setEmail("owner@orcid.org");
        user.setSalesforceId(DEFAULT_SALESFORCE_ID);
        user.setLangKey("en");
        return user;
    }

}
