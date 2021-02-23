package org.orcid.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.OrcidToken;
import org.orcid.security.EncryptUtil;
import org.orcid.service.AssertionService;
import org.orcid.service.OrcidRecordService;
import org.springframework.http.ResponseEntity;

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
		Mockito.when(assertionService.deleteAssertionFromOrcid(Mockito.eq("assertionId"))).thenReturn(Boolean.TRUE);
		ResponseEntity<String> response = assertionServiceResource.deleteAssertionFromOrcid("assertionId");
		String body = response.getBody();
		assertEquals("{\"deleted\":true}", body);
	}
	
	@Test
	void testDeleteAssertionFromOrcidFailure() throws JSONException, JAXBException {
		Mockito.when(assertionService.deleteAssertionFromOrcid(Mockito.eq("assertionId"))).thenReturn(Boolean.FALSE);
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
	

	
	private Assertion getAssertionWithError() {
		Assertion assertion = new Assertion();
		assertion.setId("assertionId");
		JSONObject error = new JSONObject();
		error.put("statusCode", 404);
		error.put("error", "not found");
		assertion.setOrcidError(error.toString());
		return assertion;
	}
	

}
