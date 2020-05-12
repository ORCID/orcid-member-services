package org.orcid.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import org.orcid.security.EncryptUtil;
import org.orcid.service.AssertionService;
import org.orcid.service.OrcidRecordService;
import org.springframework.http.ResponseEntity;

class AssertionServiceResourceTest {
	
	@Mock
    private AssertionService assertionsService;
	
	@Mock
    private OrcidRecordService orcidRecordService;

	@Mock
    private EncryptUtil encryptUtil;
	
	@InjectMocks
	private AssertionServiceResource assertionServicesResource;
	
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testDeleteAssertionFromOrcidSuccessful() throws JSONException, JAXBException {
		Mockito.when(assertionsService.deleteAssertionFromOrcid(Mockito.eq("assertionId"))).thenReturn(Boolean.TRUE);
		ResponseEntity<String> response = assertionServicesResource.deleteAssertionFromOrcid("assertionId");
		String body = response.getBody();
		assertEquals("{\"deleted\":true}", body);
	}
	
	@Test
	void testDeleteAssertionFromOrcidFailure() throws JSONException, JAXBException {
		Mockito.when(assertionsService.deleteAssertionFromOrcid(Mockito.eq("assertionId"))).thenReturn(Boolean.FALSE);
		Mockito.when(assertionsService.findById(Mockito.eq("assertionId"))).thenReturn(getAssertionWithError());
		ResponseEntity<String> response = assertionServicesResource.deleteAssertionFromOrcid("assertionId");
		String body = response.getBody();
		assertEquals("{\"deleted\":false,\"error\":\"not found\",\"statusCode\":404}", body);
	}

	@Test
	void testGetOrcidRecord() throws IOException, org.codehaus.jettison.json.JSONException {
		String encrypted = "blah";
		String email = "email@email.com";
		String encryptedOther = "nope";
		String emailOther = "nope@email.com";
		
		Mockito.when(encryptUtil.decrypt(Mockito.eq(encrypted))).thenReturn(email);
		Mockito.when(encryptUtil.decrypt(Mockito.eq(encryptedOther))).thenReturn(emailOther);
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq(email))).thenReturn(Optional.of(new OrcidRecord()));
		Mockito.when(orcidRecordService.findOneByEmail(Mockito.eq(emailOther))).thenReturn(Optional.empty());
		
		ResponseEntity<OrcidRecord> response = assertionServicesResource.getOrcidRecord(encrypted);
		assertTrue(response.getStatusCode().is2xxSuccessful());
		assertNotNull(response.getBody());
		
		Mockito.verify(encryptUtil, Mockito.times(1)).decrypt(Mockito.eq(encrypted));
		Mockito.verify(orcidRecordService, Mockito.times(1)).findOneByEmail(Mockito.eq(email));
		
		response = assertionServicesResource.getOrcidRecord(encryptedOther);
		assertTrue(response.getStatusCode().is4xxClientError());
		assertNull(response.getBody());
		
		Mockito.verify(encryptUtil, Mockito.times(1)).decrypt(Mockito.eq(encryptedOther));
		Mockito.verify(orcidRecordService, Mockito.times(1)).findOneByEmail(Mockito.eq(emailOther));
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
