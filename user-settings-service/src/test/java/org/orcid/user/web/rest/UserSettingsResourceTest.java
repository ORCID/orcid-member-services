package org.orcid.user.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Optional;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.user.client.Oauth2ServiceClient;
import org.orcid.user.domain.MemberSettings;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.repository.MemberSettingsRepository;
import org.orcid.user.repository.UserSettingsRepository;
import org.orcid.user.security.SecurityUtils;
import org.orcid.user.security.UaaUserUtils;
import org.orcid.user.service.UserService;
import org.orcid.user.service.dto.UserDTO;
import org.springframework.http.ResponseEntity;

import com.netflix.hystrix.exception.HystrixRuntimeException;

class UserSettingsResourceTest {

	@Mock
	private Oauth2ServiceClient oauth2ServiceClient;

	@Mock
	private UaaUserUtils uaaUserUtils;

	@Mock
	private UserSettingsRepository userSettingsRepository;

	@Mock
	private MemberSettingsRepository memberSettingsRepository;

	@Mock
	private SecurityUtils securityUtils;
	
	@Mock
	private UserService userService;

	@InjectMocks
	private UserSettingsResource userSettingsResource;

	@BeforeEach
	public void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Test
	void testCreateUser() throws JSONException, URISyntaxException {
		Mockito.when(memberSettingsRepository.findBySalesforceId(Mockito.eq("salesforceId")))
				.thenReturn(Optional.of(new MemberSettings()));
		Mockito.when(userService.canCreateUser(Mockito.anyString())).thenReturn(Boolean.TRUE);
		Mockito.when(oauth2ServiceClient.getUser(Mockito.anyString()))
				.thenReturn(ResponseEntity.ok().body(
						"{ 'id' : 'something', 'login' : 'login', 'createdDate' : '2020-03-26T00:42:32.655Z', 'lastModifiedBy' : 'something','lastModifiedDate' : '2020-03-26T00:42:32.655Z',  }"));
		Mockito.when(oauth2ServiceClient.registerUser(Mockito.any(HashMap.class)))
				.thenReturn(ResponseEntity.created(null).build());
		Mockito.when(userSettingsRepository.save(Mockito.anyObject())).thenReturn(getUserSettings());

		UserDTO user = getUserDTO();
		ResponseEntity<UserDTO> response = userSettingsResource.createUser(user);
		assertEquals(201, response.getStatusCode().value());

		// check no attempt made to persist MemberSettings
		Mockito.verify(memberSettingsRepository, Mockito.never()).save(Mockito.any(MemberSettings.class));
	}

	@Test
	void testCreateUserWhereMemberDoesntExist() throws JSONException, URISyntaxException {
		Mockito.when(memberSettingsRepository.findBySalesforceId(Mockito.eq("salesforceId")))
				.thenReturn(Optional.empty());
		Mockito.when(oauth2ServiceClient.getUser(Mockito.anyString()))
				.thenThrow(new HystrixRuntimeException(null, null, null, null, null))
				.thenReturn(ResponseEntity.ok().body(
						"{ 'id' : 'something', 'login' : 'login', 'createdDate' : '2020-03-26T00:42:32.655Z', 'lastModifiedBy' : 'something','lastModifiedDate' : '2020-03-26T00:42:32.655Z',  }"));

		UserDTO user = getUserDTO();
		ResponseEntity<UserDTO> response = userSettingsResource.createUser(user);
		assertEquals(400, response.getStatusCode().value());
	}

	private UserDTO getUserDTO() {
		UserDTO user = new UserDTO();
		user.setLogin("login");
		user.setFirstName("test");
		user.setLastName("test");
		user.setSalesforceId("salesforceId");
		user.setJhiUserId("jhiUserId");
		return user;
	}

	private UserSettings getUserSettings() {
		UserSettings userSettings = new UserSettings();
		userSettings.setId("id");
		return userSettings;
	}

}
