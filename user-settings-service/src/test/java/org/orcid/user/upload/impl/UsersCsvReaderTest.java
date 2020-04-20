package org.orcid.user.upload.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.orcid.user.domain.UserSettings;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.upload.UsersUpload;

class UsersCsvReaderTest {

	private UsersCsvReader reader = new UsersCsvReader();
	
	@Test
	void testReadUsersUpload() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("/users.csv");
		UsersUpload upload = reader.readUsersUpload(inputStream, "some-user");
		assertEquals(3, upload.getUserDTOs().size());
		assertEquals(3, upload.getUserSettings().size());
		
		UserDTO userDTO1 = upload.getUserDTOs().get(0);
		UserDTO userDTO2 = upload.getUserDTOs().get(1);
		UserDTO userDTO3 = upload.getUserDTOs().get(2);
		
		UserSettings userSettings1 = upload.getUserSettings().get(0);
		UserSettings userSettings2 = upload.getUserSettings().get(1);
		UserSettings userSettings3 = upload.getUserSettings().get(2);
		
		assertEquals("1@user.com", userDTO1.getLogin());
		assertEquals("2@user.com", userDTO2.getLogin());
		assertEquals("3@user.com", userDTO3.getLogin());
		
		assertEquals("Angel", userDTO1.getFirstName());
		assertEquals("Leonardo", userDTO2.getFirstName());
		assertEquals("Daniel", userDTO3.getFirstName());
		
		assertEquals("Montenegro", userDTO1.getLastName());
		assertEquals("Mendoza", userDTO2.getLastName());
		assertEquals("Palafox", userDTO3.getLastName());
		
		assertEquals("sssalesforceid1", userDTO1.getSalesforceId());
		assertEquals("salesforceid3", userDTO2.getSalesforceId());
		assertEquals("salesforceid2", userDTO3.getSalesforceId());
		
		assertTrue(userDTO1.getAssertionServicesEnabled());
		assertFalse(userDTO2.getAssertionServicesEnabled());
		assertFalse(userDTO3.getAssertionServicesEnabled());
		
		assertEquals(userDTO1.getId(), userSettings1.getJhiUserId());
		assertEquals(userDTO2.getId(), userSettings2.getJhiUserId());
		assertEquals(userDTO3.getId(), userSettings3.getJhiUserId());
	}
	
}
