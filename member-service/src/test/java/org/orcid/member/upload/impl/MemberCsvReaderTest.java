package org.orcid.member.upload.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.orcid.member.upload.MemberUpload;
import org.orcid.member.domain.Member;

class MemberCsvReaderTest {

	private MemberCsvReader reader = new MemberCsvReader();

	@Test
	void testReadMembersUpload() throws IOException {
		InputStream inputStream = getClass().getResourceAsStream("/members.csv");
		MemberUpload upload = reader.readMemberUpload(inputStream);

		assertEquals(2, upload.getMembers().size());

		Member one = upload.getMembers().get(0);
		Member two = upload.getMembers().get(1);

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
