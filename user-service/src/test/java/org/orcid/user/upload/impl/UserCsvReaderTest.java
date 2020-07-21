package org.orcid.user.upload.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.upload.UserUpload;

class UserCsvReaderTest {

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

	private UserCsvReader reader = null;

	@Test
	void testReadUsersUpload() throws IOException {
	    reader = new UserCsvReader(userRepository);

        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.empty());

        InputStream inputStream = getClass().getResourceAsStream("/users.csv");
		UserUpload upload = reader.readUsersUpload(inputStream, "some-user");
		assertEquals(3, upload.getUserDTOs().size());

		UserDTO userDTO1 = upload.getUserDTOs().get(0);
		UserDTO userDTO2 = upload.getUserDTOs().get(1);
		UserDTO userDTO3 = upload.getUserDTOs().get(2);

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

	}

}
