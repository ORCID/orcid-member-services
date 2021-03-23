package org.orcid.service.assertions.upload.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.service.AssertionService;
import org.orcid.service.assertions.upload.AssertionsUpload;

class AssertionsCsvReaderTest {

	@Mock
	private AssertionService mockAssertionService;
	
	@InjectMocks
	private AssertionsCsvReader reader;
	
	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testReadAssertionsUploadWithExternalIds() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-external-ids.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream);
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
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream);
		assertEquals(3, upload.getAssertions().size());
		assertEquals("TEST", upload.getAssertions().get(0).getOrgName());
		assertEquals("TEST-2", upload.getAssertions().get(1).getOrgName());
		assertEquals("TEST-3", upload.getAssertions().get(2).getOrgName());
		assertEquals(2, upload.getUsers().size());
	}

	@Test
	void testReadAssertionsUploadWithoutUrl() throws IOException, JSONException {
		InputStream inputStream = getClass().getResourceAsStream("/assertions-without-url.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream);
        assertEquals(3, upload.getAssertions().size());
        assertEquals("TEST", upload.getAssertions().get(0).getOrgName());
        assertEquals("TEST-2", upload.getAssertions().get(1).getOrgName());
        assertEquals("TEST-3", upload.getAssertions().get(2).getOrgName());
        assertEquals(2, upload.getUsers().size());
	}

	@Test
	void testReadAssertionsUploadWithDbIds() throws IOException {
		Mockito.when(mockAssertionService.assertionExists(Mockito.anyString())).thenReturn(true);
		
		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-db-id-column.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream);
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
		
		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-db-id-column.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream);
		
		assertEquals(1, upload.getErrors().length()); // id doesn't exist
		
		assertEquals(3, upload.getAssertions().size());  // including erroneous
		
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
		
		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-delete-row.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream);
		
		assertEquals(0, upload.getErrors().length()); // id doesn't exist
		
		assertEquals(4, upload.getAssertions().size());  // including erroneous
		
		// check fields of valid assertions
		assertEquals("id-to-delete", upload.getAssertions().get(3).getId());
		
		assertNull(upload.getAssertions().get(3).getExternalId());
		assertNull(upload.getAssertions().get(3).getAddedToORCID());
		assertNull(upload.getAssertions().get(3).getAffiliationSection());
		assertNull(upload.getAssertions().get(3).getCreated());
		assertNull(upload.getAssertions().get(3).getDeletedFromORCID());
	}

}
