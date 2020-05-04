package org.orcid.user.web.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.user.domain.MemberSettings;
import org.orcid.user.repository.MemberSettingsRepository;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.security.UaaUserUtils;
import org.orcid.user.upload.impl.MembersCsvReader;
import org.springframework.http.ResponseEntity;

class MemberSettingsResourceTest {

	@Mock
	private UaaUserUtils uaaUserUtils;

	@Mock
	private UserSettingsRepository userSettingsRepository;

	@Mock
	private MemberSettingsRepository memberSettingsRepository;

	@Mock
	private MembersCsvReader membersCsvReader;

	@InjectMocks
	private MemberSettingsResource memberSettingsResource;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testGetMemberSettings() {
		String id = "id";
		String salesforceId = "salesforceId";
		String nonExistentId = "no";

		Mockito.when(memberSettingsRepository.findById(Mockito.anyString())).thenReturn(Optional.empty());
		Mockito.when(memberSettingsRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.empty());
		Mockito.when(memberSettingsRepository.findById(Mockito.eq(id))).thenReturn(Optional.of(new MemberSettings()));
		Mockito.when(memberSettingsRepository.findById(Mockito.eq(salesforceId))).thenReturn(Optional.empty());
		Mockito.when(memberSettingsRepository.findBySalesforceId(Mockito.eq(salesforceId))).thenReturn(Optional.of(new MemberSettings()));
		
		ResponseEntity<MemberSettings> response = memberSettingsResource.getMemberSettings(id);
		assertTrue(response.getStatusCode().is2xxSuccessful());
		assertNotNull(response.getBody());
		
		Mockito.verify(memberSettingsRepository, Mockito.times(1)).findById(Mockito.eq(id));
		Mockito.verify(memberSettingsRepository, Mockito.never()).findBySalesforceId(Mockito.eq(id));
		
		response = memberSettingsResource.getMemberSettings(salesforceId);
		assertTrue(response.getStatusCode().is2xxSuccessful());
		assertNotNull(response.getBody());
		
		Mockito.verify(memberSettingsRepository, Mockito.times(1)).findById(Mockito.eq(salesforceId));
		Mockito.verify(memberSettingsRepository, Mockito.times(1)).findBySalesforceId(Mockito.eq(salesforceId));
		
		response = memberSettingsResource.getMemberSettings(nonExistentId);
		assertTrue(response.getStatusCode().is4xxClientError());
		assertNull(response.getBody());
		
		Mockito.verify(memberSettingsRepository, Mockito.times(1)).findById(Mockito.eq(nonExistentId));
		Mockito.verify(memberSettingsRepository, Mockito.times(1)).findBySalesforceId(Mockito.eq(nonExistentId));
		
	}

}
