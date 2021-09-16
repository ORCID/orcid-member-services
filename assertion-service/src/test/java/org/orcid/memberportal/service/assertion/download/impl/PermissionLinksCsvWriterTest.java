package org.orcid.memberportal.service.assertion.download.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;
import org.orcid.memberportal.service.assertion.download.impl.PermissionLinksCsvWriter;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.security.EncryptUtil;
import org.orcid.memberportal.service.assertion.services.OrcidRecordService;
import org.orcid.memberportal.service.assertion.services.UserService;

public class PermissionLinksCsvWriterTest {

    private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

    @Mock
    private EncryptUtil encryptUtil;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private OrcidRecordService orcidRecordService;

    @Mock
    private UserService assertionsUserService;

    @Mock
    private AssertionRepository assertionsRepository;

    @InjectMocks
    private PermissionLinksCsvWriter csvWriter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(applicationProperties.getLandingPageUrl()).thenReturn("https://member-portal.com");
        when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn(DEFAULT_SALESFORCE_ID);
        when(orcidRecordService.getRecordsWithoutTokens(Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(getListOfOrcidRecords());
        when(assertionsRepository.findByEmailAndSalesforceId(Mockito.anyString(), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenAnswer(new Answer<List<Assertion>>() {

            @Override
            public List<Assertion> answer(InvocationOnMock invocation) throws Throwable {
                String email = (String) invocation.getArgument(0);
                return getListOfAsserions(email);
            }

        });

        when(encryptUtil.encrypt(Mockito.anyString())).thenAnswer(new Answer<String>() {
            // just return unencrypted arg
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String arg = (String) invocation.getArgument(0);
                return arg;
            }

        });
    }

    private List<OrcidRecord> getListOfOrcidRecords() {
        List<OrcidRecord> orcidRecords = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            OrcidRecord record = new OrcidRecord();
            record.setEmail(i + "@test.com");
            record.setId(String.valueOf(i));
            orcidRecords.add(record);
        }
        return orcidRecords;
    }

    @Test
    public void testWriteAssertionsReport() throws IOException {
        String test = csvWriter.writeCsv();
        assertNotNull(test);
        assertTrue(test.length() > 0);
        String[] lines = test.split("\\n");
        assertEquals(11, lines.length); // header + permission link for each of
                                        // 10 records

        String headerLine = lines[0];
        String[] headers = headerLine.split(",");
        checkHeaders(headers);

        for (int i = 0; i < 10; i = i + 2) {
            String line = lines[i + 1];
            String[] values = line.split(",");
            checkValues(values, i);
        }
    }

    private void checkValues(String[] values, int i) {
        assertEquals(i + "@test.com", values[0].trim());
        assertEquals("https://member-portal.com?state=" + DEFAULT_SALESFORCE_ID + "&&" + i + "@test.com", values[1].trim());
    }

    private void checkHeaders(String[] headers) {
        assertEquals("email", headers[0].trim());
        assertEquals("link", headers[1].trim());
    }

    private List<Assertion> getListOfAsserions(String email) {
        List<Assertion> assertions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            assertions.add(getDummyAssertion(i, email));
        }
        return assertions;
    }

    private Assertion getDummyAssertion(int i, String email) {
        Assertion assertion = new Assertion();
        assertion.setAddedToORCID(Instant.now());
        assertion.setModified(Instant.now());
        assertion.setDepartmentName("department-" + i);
        assertion.setAffiliationSection(AffiliationSection.values()[i]);
        assertion.setEmail(email);
        assertion.setEndDay("1");
        assertion.setEndMonth("12");
        assertion.setEndYear("2015");
        assertion.setStartDay("1");
        assertion.setStartMonth("12");
        assertion.setStartYear("2010");
        assertion.setExternalId("123" + i);
        assertion.setExternalIdUrl("http://externalid/" + i);
        assertion.setId(String.valueOf(i));
        assertion.setModified(Instant.now());
        assertion.setOrgCity("city-" + i);
        assertion.setOrgCountry("US");
        assertion.setOrgName("org-" + i);
        assertion.setOrgRegion("region-" + i);
        assertion.setOwnerId("what?" + i);
        assertion.setRoleTitle("role-" + i);
        assertion.setStatus("not sure");
        assertion.setUpdatedInORCID(Instant.now());
        assertion.setDisambiguationSource("source-" + i);
        assertion.setDisambiguatedOrgId("id-" + i);
        assertion.setExternalId("extId-" + i);
        assertion.setExternalIdType("extIdType-" + i);
        assertion.setExternalIdUrl("extIdUrl-" + i);
        assertion.setUrl("url-" + i);
        assertion.setId("affiliation-id-" + i);
        return assertion;
    }

}
