package org.orcid.service.assertions.upload.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jettison.json.JSONException;
import org.junit.jupiter.api.Test;
import org.orcid.service.assertions.upload.AssertionsUpload;

class AssertionsCsvReaderTest {

	private AssertionsCsvReader reader = new AssertionsCsvReader();

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
		InputStream inputStream = getClass().getResourceAsStream("/assertions-with-db-id-column.csv");
		AssertionsUpload upload = reader.readAssertionsUpload(inputStream);
		assertEquals(2, upload.getAssertions().size());
		assertEquals("ORCID", upload.getAssertions().get(0).getOrgName());
		assertEquals("ext-id", upload.getAssertions().get(0).getExternalId());
		assertEquals("a-database-id", upload.getAssertions().get(0).getId());
		
		assertEquals(2, upload.getAssertions().size());
		assertEquals("ORCID-2", upload.getAssertions().get(1).getOrgName());
		assertEquals("ext-id-2", upload.getAssertions().get(1).getExternalId());
		assertNull(upload.getAssertions().get(1).getId());

		// check http:// protocol has been added to url
		assertEquals("http://bbc.co.uk", upload.getAssertions().get(0).getUrl());
		assertEquals(1, upload.getUsers().size());
	}

}
