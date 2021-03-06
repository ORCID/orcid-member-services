package org.orcid.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.OrcidToken;
import org.orcid.domain.enumeration.AffiliationSection;
import org.orcid.security.EncryptUtil;
import org.orcid.service.AssertionService;
import org.orcid.service.OrcidRecordService;
import org.orcid.service.assertions.upload.AssertionsUploadSummary;
import org.orcid.web.rest.errors.BadRequestAlertException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

class AssertionServiceResourceTest {
    
    private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";
	
	@Mock
    private AssertionService assertionService;
	
	@Mock
    private OrcidRecordService orcidRecordService;

	@Mock
    private EncryptUtil encryptUtil;
	
	@InjectMocks
	private AssertionServiceResource assertionServiceResource;
	
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testDeleteAssertionFromOrcidSuccessful() throws JSONException, JAXBException {
		Mockito.when(assertionService.deleteAssertionFromOrcidRegistry(Mockito.eq("assertionId"))).thenReturn(Boolean.TRUE);
		ResponseEntity<String> response = assertionServiceResource.deleteAssertionFromOrcid("assertionId");
		String body = response.getBody();
		assertEquals("{\"deleted\":true}", body);
	}
	
	@Test
	void testDeleteAssertionFromOrcidFailure() throws JSONException, JAXBException {
		Mockito.when(assertionService.deleteAssertionFromOrcidRegistry(Mockito.eq("assertionId"))).thenReturn(Boolean.FALSE);
		Mockito.when(assertionService.findById(Mockito.eq("assertionId"))).thenReturn(getAssertionWithError());
		ResponseEntity<String> response = assertionServiceResource.deleteAssertionFromOrcid("assertionId");
		String body = response.getBody();
		assertEquals("{\"deleted\":false,\"error\":\"not found\",\"statusCode\":404}", body);
	}

	@Test
	void testGetOrcidRecord() throws IOException, org.codehaus.jettison.json.JSONException {
	        String email = "email@email.com";
	        String encrypted = encryptUtil.encrypt(DEFAULT_SALESFORCE_ID + "&&" + email);
	        
		OrcidRecord record = new OrcidRecord();
		List<OrcidToken> tokens = new ArrayList<OrcidToken>();
                OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken", null, null);
                tokens.add(newToken);
                record.setTokens(tokens);
		
		Mockito.when(encryptUtil.decrypt(Mockito.eq(encrypted))).thenReturn(DEFAULT_SALESFORCE_ID + "&&" + email);
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq(email))).thenReturn(Optional.of(record));
				
		ResponseEntity<OrcidRecord> response = assertionServiceResource.getOrcidRecord(encrypted);
		assertTrue(response.getStatusCode().is2xxSuccessful());
		assertNotNull(response.getBody());
		
		Mockito.verify(encryptUtil, Mockito.times(1)).decrypt(Mockito.eq(encrypted));
		Mockito.verify(orcidRecordService, Mockito.times(1)).findOneByEmail(Mockito.eq(email));
		
		String emailOther = "nope@email.com";
                String encryptedOther = encryptUtil.encrypt(DEFAULT_SALESFORCE_ID + "&&" + emailOther);
		
	        Mockito.when(encryptUtil.decrypt(Mockito.eq(encryptedOther))).thenReturn(DEFAULT_SALESFORCE_ID + "&&" + emailOther);
	        Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq(emailOther))).thenReturn(Optional.empty());

		response = assertionServiceResource.getOrcidRecord(encryptedOther);
		assertTrue(response.getStatusCode().is4xxClientError());
		assertNull(response.getBody());
		
	}
	
	@Test
	void testGenerateCsv() throws IOException {
		Mockito.when(assertionService.generateAssertionsCSV()).thenReturn("test");
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		ServletOutputStream outputStream = Mockito.mock(ServletOutputStream.class);
		Mockito.when(response.getOutputStream()).thenReturn(outputStream);
		
		assertionServiceResource.generateCsv(response);
		
		ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
		Mockito.verify(outputStream, Mockito.times(1)).write(bodyCaptor.capture());
		String body = new String(bodyCaptor.getValue(), "UTF-8");
		assertEquals("test", body);
		
		ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
		Mockito.verify(response, Mockito.times(3)).setHeader(headerNameCaptor.capture(), headerValueCaptor.capture());
		
		List<String> headerNames = headerNameCaptor.getAllValues();
		assertEquals("Content-Disposition", headerNames.get(0));
		assertEquals("Content-Type", headerNames.get(1));
		assertEquals("filename", headerNames.get(2));
		
		List<String> headerValues = headerValueCaptor.getAllValues();
		assertTrue(headerValues.get(0).startsWith("attachment; filename="));
		assertEquals("text/csv", headerValues.get(1));
		assertTrue(headerValues.get(2).endsWith("affiliations.csv"));
	}
	
	@Test
	void testGenerateReport() throws IOException {
		Mockito.when(assertionService.generateAssertionsReport()).thenReturn("test");
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		ServletOutputStream outputStream = Mockito.mock(ServletOutputStream.class);
		Mockito.when(response.getOutputStream()).thenReturn(outputStream);
		
		assertionServiceResource.generateReport(response);
		
		ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
		Mockito.verify(outputStream, Mockito.times(1)).write(bodyCaptor.capture());
		String body = new String(bodyCaptor.getValue(), "UTF-8");
		assertEquals("test", body);
		
		ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
		Mockito.verify(response, Mockito.times(3)).setHeader(headerNameCaptor.capture(), headerValueCaptor.capture());
		
		List<String> headerNames = headerNameCaptor.getAllValues();
		assertEquals("Content-Disposition", headerNames.get(0));
		assertEquals("Content-Type", headerNames.get(1));
		assertEquals("filename", headerNames.get(2));
		
		List<String> headerValues = headerValueCaptor.getAllValues();
		assertTrue(headerValues.get(0).startsWith("attachment; filename="));
		assertEquals("text/csv", headerValues.get(1));
		assertTrue(headerValues.get(2).endsWith("orcid_report.csv"));
	}
	
	@Test
	void testGenerateLinks() throws Exception {
		Mockito.when(assertionService.generatePermissionLinks()).thenReturn("test");
		HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
		ServletOutputStream outputStream = Mockito.mock(ServletOutputStream.class);
		Mockito.when(response.getOutputStream()).thenReturn(outputStream);
		
		assertionServiceResource.generatePermissionLinks(response);
		
		ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
		Mockito.verify(outputStream, Mockito.times(1)).write(bodyCaptor.capture());
		String body = new String(bodyCaptor.getValue(), "UTF-8");
		assertEquals("test", body);
		
		ArgumentCaptor<String> headerNameCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);
		Mockito.verify(response, Mockito.times(3)).setHeader(headerNameCaptor.capture(), headerValueCaptor.capture());
		
		List<String> headerNames = headerNameCaptor.getAllValues();
		assertEquals("Content-Disposition", headerNames.get(0));
		assertEquals("Content-Type", headerNames.get(1));
		assertEquals("filename", headerNames.get(2));
		
		List<String> headerValues = headerValueCaptor.getAllValues();
		assertTrue(headerValues.get(0).startsWith("attachment; filename="));
		assertEquals("text/csv", headerValues.get(1));
		assertTrue(headerValues.get(2).endsWith("orcid_permission_links.csv"));
	}
	
	@Test
	void testCreateAssertion() throws BadRequestAlertException, URISyntaxException {
		Assertion creatingAssertion = getAssertion("test-create-assertion@orcid.org");
		Assertion createdAssertion = getAssertion("test-create-assertion@orcid.org");
		createdAssertion.setId("some-id-because-this-assertion-exists-already");
		Mockito.when(assertionService.isDuplicate(Mockito.any(Assertion.class))).thenReturn(false);
		Mockito.when(assertionService.createAssertion(Mockito.any(Assertion.class))).thenReturn(createdAssertion);
		ResponseEntity<Assertion> response = assertionServiceResource.createAssertion(creatingAssertion);
		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		Mockito.verify(assertionService, Mockito.times(1)).createAssertion(Mockito.any(Assertion.class));
		Mockito.verify(assertionService, Mockito.times(1)).isDuplicate(Mockito.any(Assertion.class));
	}
	
	@Test
	void testCreateAssertionInvalidEmail() throws BadRequestAlertException, URISyntaxException {
		Assertion creatingAssertion = getAssertion("test-create-assertion@orcid");
		Assertions.assertThrows(BadRequestAlertException.class, () -> {
			assertionServiceResource.createAssertion(creatingAssertion);
		});
	}
	
	@Test
	void testUpdateAssertion() throws BadRequestAlertException, URISyntaxException, org.codehaus.jettison.json.JSONException {
		Assertion assertion = getAssertion("test-update-assertion@orcid.org");
		assertion.setId("some-id-because-this-assertion-exists-already");
		Mockito.when(assertionService.isDuplicate(Mockito.any(Assertion.class))).thenReturn(false);
		Mockito.when(assertionService.updateAssertion(Mockito.any(Assertion.class))).thenReturn(assertion);
		ResponseEntity<Assertion> response = assertionServiceResource.updateAssertion(assertion);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		Mockito.verify(assertionService, Mockito.times(1)).updateAssertion(Mockito.any(Assertion.class));
		Mockito.verify(assertionService, Mockito.times(1)).isDuplicate(Mockito.any(Assertion.class));
	}
	
	@Test
	void testUpdateAssertionInvalidEmail() throws BadRequestAlertException, URISyntaxException {
		Assertion updatingAssertion = getAssertion("test-create-assertion@orcid");
		updatingAssertion.setId("some-id-because-this-assertion-exists-already");
		Assertions.assertThrows(BadRequestAlertException.class, () -> {
			assertionServiceResource.updateAssertion(updatingAssertion);
		});
	}
	
	@Test
	void testCreateDuplicateAssertion() throws BadRequestAlertException, URISyntaxException {
		Assertion creatingAssertion = getAssertion("test-create-assertion@orcid.org");
		Mockito.when(assertionService.isDuplicate(Mockito.any(Assertion.class))).thenReturn(true);
		
		Assertions.assertThrows(BadRequestAlertException.class, () -> {
			assertionServiceResource.createAssertion(creatingAssertion);
		});
		
		Mockito.verify(assertionService, Mockito.never()).createAssertion(Mockito.any(Assertion.class));
		Mockito.verify(assertionService, Mockito.times(1)).isDuplicate(Mockito.any(Assertion.class));
	}
	
	@Test
	void testUpdateDuplicateAssertion() throws BadRequestAlertException, URISyntaxException {
		Assertion assertion = getAssertion("test-update-assertion@orcid.org");
		Mockito.when(assertionService.isDuplicate(Mockito.any(Assertion.class))).thenReturn(true);
		
		Assertions.assertThrows(BadRequestAlertException.class, () -> {
			assertionServiceResource.updateAssertion(assertion);
		});
		
		Mockito.verify(assertionService, Mockito.never()).updateAssertion(Mockito.any(Assertion.class));
		Mockito.verify(assertionService, Mockito.times(1)).isDuplicate(Mockito.any(Assertion.class));
	}
	
	@Test
	void testUploadAssertions() {
		MultipartFile file = Mockito.mock(MultipartFile.class);
		Mockito.when(assertionService.uploadAssertions(Mockito.any())).thenReturn(new AssertionsUploadSummary());
		assertionServiceResource.uploadAssertions(file);
		Mockito.verify(assertionService, Mockito.times(1)).uploadAssertions(Mockito.any());
	}
	
	private Assertion getAssertion(String email) {
		Assertion assertion = new Assertion();
		assertion.setAffiliationSection(AffiliationSection.DISTINCTION);
		assertion.setOrgName("org");
		assertion.setOrgCountry("US");
		assertion.setOrgCity("city");
		assertion.setDisambiguatedOrgId("something");
		assertion.setDisambiguationSource("some source");
		assertion.setEmail(email);
		return assertion;
	}
	
	private Assertion getAssertionWithError() {
		Assertion assertion = getAssertion("error@error.com");
		assertion.setId("assertionId");
		JSONObject error = new JSONObject();
		error.put("statusCode", 404);
		error.put("error", "not found");
		assertion.setOrcidError(error.toString());
		return assertion;
	}
	

}
