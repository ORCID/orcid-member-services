package org.orcid.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.user.client.Oauth2ServiceClient;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.repository.UserSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.netflix.hystrix.exception.HystrixRuntimeException;

class UserServiceImplTest {

	@Mock
	private UserSettingsRepository userSettingsRepository;

	@Mock
	private Oauth2ServiceClient oauth2ServiceClient;

	@InjectMocks
	private UserServiceImpl userServiceImpl;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	/**
	 * Checks a user can be created for the first time
	 */
	@Test
	void testCanCreateUserNoJhiUser() {
		Mockito.when(oauth2ServiceClient.getUser(Mockito.eq("login"))).thenThrow(
				new HystrixRuntimeException(null, null, null, new ResponseStatusException(HttpStatus.NOT_FOUND), null));
		assertTrue(userServiceImpl.canCreateUser("login"));
	}

	/**
	 * Checks a previously deleted user can be created
	 */
	@Test
	void testCanCreateUserPreviouslyDeleted() {
		Mockito.when(oauth2ServiceClient.getUser(Mockito.eq("login"))).thenReturn(getJhiUserResponse());
		Mockito.when(userSettingsRepository.findByJhiUserId(Mockito.eq("something"))).thenReturn(getDeletedUserSettings());
		assertTrue(userServiceImpl.canCreateUser("login"));
	}
	
	/**
	 * Checks existing user can't be created
	 */
	@Test
	void testCanCreateUserNotPreviouslyDeleted() {
		Mockito.when(oauth2ServiceClient.getUser(Mockito.eq("login"))).thenReturn(getJhiUserResponse());
		Mockito.when(userSettingsRepository.findByJhiUserId(Mockito.eq("something"))).thenReturn(getExistingUserSettings());
		assertFalse(userServiceImpl.canCreateUser("login"));
	}

	private Optional<UserSettings> getExistingUserSettings() {
		UserSettings userSettings = new UserSettings();
		userSettings.setJhiUserId("something");
		userSettings.setDeleted(Boolean.FALSE);
		return Optional.of(userSettings);
	}

	private Optional<UserSettings> getDeletedUserSettings() {
		UserSettings userSettings = new UserSettings();
		userSettings.setJhiUserId("something");
		userSettings.setDeleted(Boolean.TRUE);
		return Optional.of(userSettings);
	}

	private ResponseEntity<String> getJhiUserResponse() {
		return ResponseEntity.ok().body(
				"{ 'id' : 'something', 'login' : 'login', 'createdDate' : '2020-03-26T00:42:32.655Z', 'lastModifiedBy' : 'something','lastModifiedDate' : '2020-03-26T00:42:32.655Z',  }");
	}
}
