package org.orcid.service.assertions.upload.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.domain.Assertion;
import org.orcid.domain.AssertionServiceUser;
import org.orcid.service.AssertionService;
import org.orcid.service.assertions.upload.AssertionsUpload;
import org.springframework.context.MessageSource;

class AssertionsCsvReaderTest {

	@Mock
	private AssertionService mockAssertionService;

	@Mock
	private MessageSource messageSource;

	@InjectMocks
	private AssertionsCsvReader reader;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testReadAssertionsUploadWithExternalIds() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-external-ids.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));
		assertEquals(1, upload.getAssertions().size());
		assertEquals("ORCID", upload.getAssertions().get(0).getOrgName());
		assertEquals("ext-id", upload.getAssertions().get(0).getExternalId());

		// check http:// protocol has been added to url
		assertEquals("http://bbc.co.uk", upload.getAssertions().get(0).getUrl());
		assertEquals(1, upload.getUsers().size());
	}

	@Test
	void testReadAssertionsUploadWithoutExternalIds() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("/assertions-without-external-ids.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));
		assertEquals(3, upload.getAssertions().size());
		assertEquals("TEST", upload.getAssertions().get(0).getOrgName());
		assertEquals("TEST-2", upload.getAssertions().get(1).getOrgName());
		assertEquals("TEST-3", upload.getAssertions().get(2).getOrgName());
		assertEquals(2, upload.getUsers().size());
	}

	@Test
	void testReadAssertionsUploadWithStartDateAfterEndDate() throws IOException {
		Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.eq(Locale.ENGLISH)))
				.thenReturn("some-value");

		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-start-date-after-end-date.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));
		assertEquals(6, upload.getAssertions().size());
		assertEquals("TEST", upload.getAssertions().get(0).getOrgName());
		assertEquals("TEST-2", upload.getAssertions().get(1).getOrgName());
		assertEquals("TEST-3", upload.getAssertions().get(2).getOrgName());

		assertEquals(4, upload.getErrors().size());
		assertTrue(upload.getErrors().get(0).getMessage().contains("some-value"));
		assertTrue(upload.getErrors().get(1).getMessage().contains("some-value"));
		assertTrue(upload.getErrors().get(2).getMessage().contains("some-value"));
		assertTrue(upload.getErrors().get(3).getMessage().contains("some-value"));

		Mockito.verify(messageSource, Mockito.times(4)).getMessage(
				Mockito.eq("assertion.csv.upload.error.startDateAfterEndDate"), Mockito.isNull(), Mockito.eq(Locale.ENGLISH));
	}

	@Test
	void testReadAssertionsUploadWithoutUrl() throws IOException, JSONException {
		InputStream inputStream = getClass().getResourceAsStream("/assertions-without-url.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));
		assertEquals(3, upload.getAssertions().size());
		assertEquals("TEST", upload.getAssertions().get(0).getOrgName());
		assertEquals("TEST-2", upload.getAssertions().get(1).getOrgName());
		assertEquals("TEST-3", upload.getAssertions().get(2).getOrgName());
		assertEquals(2, upload.getUsers().size());
	}

	@Test
	void testReadAssertionsUploadWithBadUrl() throws IOException, JSONException {
		Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.eq(Locale.FRENCH)))
				.thenReturn("some-value");

		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-bad-url.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("fr"));
		assertEquals(1, upload.getErrors().size());
		assertTrue(upload.getErrors().get(0).getMessage().contains("some-value"));

		Mockito.verify(messageSource, Mockito.times(1)).getMessage(Mockito.eq("assertion.csv.upload.error.invalidUrl"),
				Mockito.isNull(), Mockito.eq(Locale.FRENCH));
	}

	@Test
	void testReadAssertionsUploadWithBadEmail() throws IOException, JSONException {
		Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.eq(Locale.ENGLISH)))
				.thenReturn("some-value");

		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-bad-email.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));
		assertEquals(1, upload.getErrors().size());
		assertTrue(upload.getErrors().get(0).getMessage().contains("some-value"));

		Mockito.verify(messageSource, Mockito.times(1))
				.getMessage(Mockito.eq("assertion.csv.upload.error.invalidEmail"), Mockito.isNull(), Mockito.eq(Locale.ENGLISH));
	}

	@Test
	void testReadAssertionsUploadWithDbIds() throws IOException {
		Mockito.when(mockAssertionService.assertionExists(Mockito.anyString())).thenReturn(true);
		Mockito.when(mockAssertionService.findById(Mockito.anyString())).thenReturn(getDummyAssertionWithEmail());

		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-db-id-column.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));
		assertEquals(3, upload.getAssertions().size());
		assertEquals("ORCID", upload.getAssertions().get(0).getOrgName());
		assertEquals("ext-id", upload.getAssertions().get(0).getExternalId());
		assertEquals("a-database-id", upload.getAssertions().get(0).getId());

		assertEquals("ORCID", upload.getAssertions().get(1).getOrgName());
		assertEquals("ext-id", upload.getAssertions().get(1).getExternalId());
		assertEquals("another-database-id", upload.getAssertions().get(1).getId());

		assertEquals("ORCID-2", upload.getAssertions().get(2).getOrgName());
		assertEquals("ext-id-2", upload.getAssertions().get(2).getExternalId());
		assertNull(upload.getAssertions().get(2).getId());

		// check http:// protocol has been added to url
		assertEquals("http://bbc.co.uk", upload.getAssertions().get(0).getUrl());
		assertEquals(1, upload.getUsers().size());
	}

	@Test
	void testReadAssertionsUploadWithAddedWhitespace() throws IOException {
		Mockito.when(mockAssertionService.assertionExists(Mockito.anyString())).thenReturn(true);
		Mockito.when(mockAssertionService.findById(Mockito.anyString())).thenReturn(getDummyAssertionWithEmail());

		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-whitespace.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));
		assertEquals(3, upload.getAssertions().size());
		assertEquals("ORCID", upload.getAssertions().get(0).getOrgName());
		assertEquals("ext-id", upload.getAssertions().get(0).getExternalId());
		assertEquals("a-database-id", upload.getAssertions().get(0).getId());

		assertEquals("ORCID", upload.getAssertions().get(1).getOrgName());
		assertEquals("ext-id", upload.getAssertions().get(1).getExternalId());
		assertEquals("another-database-id", upload.getAssertions().get(1).getId());

		assertEquals("ORCID-2", upload.getAssertions().get(2).getOrgName());
		assertEquals("ext-id-2", upload.getAssertions().get(2).getExternalId());
		assertNull(upload.getAssertions().get(2).getId());

		// check http:// protocol has been added to url
		assertEquals("http://bbc.co.uk", upload.getAssertions().get(0).getUrl());
		assertEquals(1, upload.getUsers().size());
	}

	@Test
	void testReadAssertionsUploadWithError() throws IOException {
		Mockito.when(mockAssertionService.assertionExists(Mockito.eq("a-database-id"))).thenReturn(false);
		Mockito.when(mockAssertionService.assertionExists(Mockito.eq("another-database-id"))).thenReturn(true);
		Mockito.when(mockAssertionService.findById(Mockito.anyString())).thenReturn(getDummyAssertionWithEmail());

		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-db-id-column.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));

		assertEquals(1, upload.getErrors().size()); // id doesn't exist

		assertEquals(3, upload.getAssertions().size()); // including erroneous

		// check fields of valid assertions
		assertEquals("ORCID", upload.getAssertions().get(1).getOrgName());
		assertEquals("ext-id", upload.getAssertions().get(1).getExternalId());
		assertEquals("another-database-id", upload.getAssertions().get(1).getId());

		assertEquals("ORCID-2", upload.getAssertions().get(2).getOrgName());
		assertEquals("ext-id-2", upload.getAssertions().get(2).getExternalId());
		assertNull(upload.getAssertions().get(2).getId());

		// check http:// protocol has been added to url
		assertEquals("http://bbc.co.uk", upload.getAssertions().get(1).getUrl());
		assertEquals(1, upload.getUsers().size());
	}

	@Test
	void testReadAssertionsUploadWithDeleteRow() throws IOException {
		Mockito.when(mockAssertionService.assertionExists(Mockito.eq("a-database-id"))).thenReturn(true);
		Mockito.when(mockAssertionService.assertionExists(Mockito.eq("another-database-id"))).thenReturn(true);
		Mockito.when(mockAssertionService.assertionExists(Mockito.eq("id-to-delete"))).thenReturn(true);
		Mockito.when(mockAssertionService.findById(Mockito.anyString())).thenReturn(getDummyAssertionWithEmail());

		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-delete-row.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));

		assertEquals(0, upload.getErrors().size()); // id doesn't exist

		assertEquals(4, upload.getAssertions().size()); // including erroneous

		// check fields of valid assertions
		assertEquals("id-to-delete", upload.getAssertions().get(3).getId());

		assertNull(upload.getAssertions().get(3).getExternalId());
		assertNull(upload.getAssertions().get(3).getAddedToORCID());
		assertNull(upload.getAssertions().get(3).getAffiliationSection());
		assertNull(upload.getAssertions().get(3).getCreated());
		assertNull(upload.getAssertions().get(3).getDeletedFromORCID());
	}

	@Test
	void testReadAssertionsUploadWithUpdatedEmail() throws IOException, JSONException {
		Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.eq(Locale.ENGLISH)))
				.thenReturn("some-value");

		Mockito.when(mockAssertionService.assertionExists(Mockito.eq("a-database-id"))).thenReturn(true);
		Mockito.when(mockAssertionService.assertionExists(Mockito.eq("another-database-id"))).thenReturn(true);

		Assertion existingAssertionWithDifferentEmail = new Assertion();
		existingAssertionWithDifferentEmail.setEmail("different@email.com");
		existingAssertionWithDifferentEmail.setId("another-database-id");

		Mockito.when(mockAssertionService.findById(Mockito.eq("a-database-id")))
				.thenReturn(getDummyAssertionWithEmail());
		Mockito.when(mockAssertionService.findById(Mockito.eq("another-database-id")))
				.thenReturn(existingAssertionWithDifferentEmail);

		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-db-id-column.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));

		assertEquals(1, upload.getErrors().size()); // email can't be changed
		assertTrue(upload.getErrors().get(0).getMessage().contains("some-value"));
		assertEquals(3, upload.getAssertions().size()); // including erroneous

		Mockito.verify(messageSource, Mockito.times(1)).getMessage(
				Mockito.eq("assertion.csv.upload.error.emailCannotBeChanged"), Mockito.isNull(), Mockito.eq(Locale.ENGLISH));
	}

	@Test
	void testReadAssertionsWithInterestingDates() throws IOException {
		Mockito.when(mockAssertionService.assertionExists(Mockito.eq("a-database-id"))).thenReturn(true);
		Mockito.when(mockAssertionService.assertionExists(Mockito.eq("another-database-id"))).thenReturn(true);
		Mockito.when(mockAssertionService.findById(Mockito.anyString())).thenReturn(getDummyAssertionWithEmail());

		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-interesting-dates.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream, getUser("en"));

		assertEquals(0, upload.getErrors().size());
		assertEquals(3, upload.getAssertions().size()); // including erroneous

		assertEquals("2020", upload.getAssertions().get(0).getStartYear());
		assertEquals("01", upload.getAssertions().get(0).getStartMonth());
		assertEquals("01", upload.getAssertions().get(0).getStartDay());
		assertEquals("2021", upload.getAssertions().get(0).getEndYear());
		assertEquals("03", upload.getAssertions().get(0).getEndMonth());
		assertEquals("05", upload.getAssertions().get(0).getEndDay());

		assertEquals("2020", upload.getAssertions().get(1).getStartYear());
		assertEquals("02", upload.getAssertions().get(1).getStartMonth());
		assertEquals("03", upload.getAssertions().get(1).getStartDay());
		assertNull(upload.getAssertions().get(1).getEndYear());
		assertNull(upload.getAssertions().get(1).getEndMonth());
		assertNull(upload.getAssertions().get(1).getEndDay());

		assertNull(upload.getAssertions().get(2).getStartYear());
		assertNull(upload.getAssertions().get(2).getStartMonth());
		assertNull(upload.getAssertions().get(2).getStartDay());
		assertNull(upload.getAssertions().get(2).getEndYear());
		assertNull(upload.getAssertions().get(2).getEndMonth());
		assertNull(upload.getAssertions().get(2).getEndDay());
	}

	private Assertion getDummyAssertionWithEmail() {
		Assertion dummy = new Assertion();
		dummy.setEmail("email@orcid.org");
		return dummy;
	}

	private AssertionServiceUser getUser(String langKey) {
		AssertionServiceUser user = new AssertionServiceUser();
		user.setId("some-id");
		user.setLangKey(langKey);
		user.setEmail("something@orcid.org");
		user.setLoginAs("something@orcid.org");
		user.setSalesforceId("something");
		return user;
	}

}
