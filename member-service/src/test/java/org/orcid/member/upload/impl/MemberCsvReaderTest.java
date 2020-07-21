package org.orcid.member.upload.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.member.repository.MemberRepository;
import org.orcid.member.upload.MemberUpload;
import org.orcid.member.domain.Member;

class MemberCsvReaderTest {

    @Mock
    MemberRepository memberRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private MemberCsvReader reader = null;

	@Test
	void testReadMembersUpload() throws IOException {
        reader = new MemberCsvReader(memberRepository);

        Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.empty());

        InputStream inputStream = getClass().getResourceAsStream("/members.csv");
		MemberUpload upload = reader.readMemberUpload(inputStream);

		assertEquals(2, upload.getMembers().size());

		Member one = upload.getMembers().get(0);
		Member two = upload.getMembers().get(1);

		assertTrue(one.getAssertionServiceEnabled());
		assertFalse(two.getAssertionServiceEnabled());

		assertEquals("XXXX-XXXX-XXXX-XXX1", one.getClientId());
		assertEquals("XXXX-XXXX-XXXX-XXX2", two.getClientId());

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
