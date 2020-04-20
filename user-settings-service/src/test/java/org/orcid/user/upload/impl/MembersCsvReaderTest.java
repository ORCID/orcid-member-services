package org.orcid.user.upload.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.orcid.user.domain.MemberSettings;
import org.orcid.user.upload.MembersUpload;

class MembersCsvReaderTest {

	private MembersCsvReader reader = new MembersCsvReader();

	@Test
	void testReadMembersUpload() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("/members.csv");
		MembersUpload upload = reader.readMembersUpload(inputStream);

		assertEquals(2, upload.getMembers().size());

		MemberSettings one = upload.getMembers().get(0);
		MemberSettings two = upload.getMembers().get(1);

		assertTrue(one.getAssertionServiceEnabled());
		assertFalse(two.getAssertionServiceEnabled());

		assertEquals("client-id-1", one.getClientId());
		assertEquals("client-id-2", two.getClientId());

		assertFalse(one.getIsConsortiumLead());
		assertFalse(two.getIsConsortiumLead());

		assertEquals("salesforce-1", one.getSalesforceId());
		assertEquals("salesforce-2", two.getSalesforceId());

		assertEquals("salesforce-parent", one.getParentSalesforceId());
		assertEquals("salesforce-parent", two.getParentSalesforceId());

		assertEquals("some-client-name", one.getClientName());
		assertEquals("some-other-client-name", two.getClientName());
	}

}
