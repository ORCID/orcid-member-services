package org.orcid.memberportal.service.assertion.csv.download.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.OrcidToken;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.domain.normalization.AssertionNormalizer;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.services.OrcidRecordService;
import org.orcid.memberportal.service.assertion.services.UserService;
import org.springframework.data.domain.Sort;

public class AssertionsReportCsvWriterTest {

    private static final String DEFAULT_JHI_USER_ID = "user-id";

    private static final String DEFAULT_LOGIN = "user@orcid.org";

    private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

    @Mock
    private AssertionRepository assertionsRepository;

    @Mock
    private UserService assertionsUserService;

    @Mock
    private OrcidRecordService orcidRecordService;
    
    @Mock
    private AssertionNormalizer assertionNormalizer;

    @InjectMocks
    private AssertionsReportCsvWriter reportWriter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn(DEFAULT_SALESFORCE_ID);
        when(assertionsRepository.findBySalesforceId(Mockito.eq(DEFAULT_SALESFORCE_ID), Mockito.any(Sort.class))).thenReturn(getListOfAsserions());
        when(assertionsUserService.getLoggedInUser()).thenReturn(getUser());
        when(assertionsUserService.getLoggedInUserId()).thenReturn(getUser().getId());
        Mockito.when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenAnswer(new Answer<Optional<OrcidRecord>>() {

            @Override
            public Optional<OrcidRecord> answer(InvocationOnMock invocation) throws Throwable {
                return getDummyOrcidRecord(invocation.getArgument(0));
            }

        });
    }

    private AssertionServiceUser getUser() {
        AssertionServiceUser user = new AssertionServiceUser();
        user.setId(DEFAULT_JHI_USER_ID);
        user.setEmail(DEFAULT_LOGIN);
        user.setSalesforceId(DEFAULT_SALESFORCE_ID);
        return user;
    }

    @Test
    public void testWriteAssertionsReport() throws IOException {
        String test = reportWriter.writeCsv(assertionsUserService.getLoggedInUserSalesforceId());
        assertNotNull(test);
        assertTrue(test.length() > 0);

        String[] lines = test.split("\\n");
        assertEquals(6, lines.length); // header + 5 lines of data

        String headerLine = lines[0];
        String[] headers = headerLine.split(",");
        checkHeaders(headers);

        for (int i = 0; i < 5; i++) {
            String line = lines[i + 1];
            String[] values = line.split(",");
            checkValues(values, i);
        }
    }

    private void checkValues(String[] values, int i) {
        assertEquals(i + "@test.com", values[0].trim());
        assertEquals("orcid-" + i, values[1].trim());
        assertEquals("Pending", values[2].trim());
        assertEquals(String.valueOf(i), values[3].trim());
        assertNotNull(values[4].trim());
        assertNotNull(values[5].trim());
        assertEquals(AffiliationSection.values()[i].toString(), values[6].trim());
        assertEquals("department-" + i, values[7].trim());
        assertEquals("role-" + i, values[8].trim());
        assertEquals("2010-12-1", values[9].trim());
        assertEquals("2015-12-1", values[10].trim());
        assertEquals("org-" + i, values[11].trim());
        assertEquals("US", values[12].trim());
        assertEquals("city-" + i, values[13].trim());
        assertEquals("region-" + i, values[14].trim());
        assertEquals("disambiguated-id-" + i, values[15].trim());
        assertEquals("disambiguation-source-" + i, values[16].trim());
        assertEquals("123" + i, values[17].trim());
        assertEquals("extIdType-" + i, values[18].trim());
        assertEquals("extIdUrl-" + i, values[19].trim());
    }

    private void checkHeaders(String[] headers) {
        assertEquals("email", headers[0].trim());
        assertEquals("orcid", headers[1].trim());
        assertEquals("status", headers[2].trim());
        assertEquals("putCode", headers[3].trim());
        assertEquals("created", headers[4].trim());
        assertEquals("modified", headers[5].trim());
        assertEquals("affiliation-section", headers[6].trim());
        assertEquals("department-name", headers[7].trim());
        assertEquals("role-title", headers[8].trim());
        assertEquals("start-date", headers[9].trim());
        assertEquals("end-date", headers[10].trim());
        assertEquals("org-name", headers[11].trim());
        assertEquals("org-country", headers[12].trim());
        assertEquals("org-city", headers[13].trim());
        assertEquals("org-region", headers[14].trim());
        assertEquals("disambiguated-organization-identifier", headers[15].trim());
        assertEquals("disambiguation-source", headers[16].trim());
        assertEquals("external-id", headers[17].trim());
        assertEquals("external-id-type", headers[18].trim());
        assertEquals("external-id-url", headers[19].trim());

    }

    private List<Assertion> getListOfAsserions() {
        List<Assertion> assertions = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            assertions.add(getDummyAssertion(i));
        }
        return assertions;
    }

    private Assertion getDummyAssertion(int i) {
        Assertion assertion = new Assertion();
        assertion.setAddedToORCID(Instant.now());
        assertion.setModified(Instant.now());
        assertion.setDepartmentName("department-" + i);
        assertion.setAffiliationSection(AffiliationSection.values()[i]);
        assertion.setEmail(i + "@test.com");
        assertion.setEndDay("1");
        assertion.setEndMonth("12");
        assertion.setEndYear("2015");
        assertion.setStartDay("1");
        assertion.setStartMonth("12");
        assertion.setStartYear("2010");
        assertion.setExternalId("123" + i);
        assertion.setExternalIdUrl("extIdUrl-" + i);
        assertion.setExternalIdType("extIdType-" + i);
        assertion.setId(String.valueOf(i));
        assertion.setModified(Instant.now());
        assertion.setOrgCity("city-" + i);
        assertion.setOrgCountry("US");
        assertion.setOrgName("org-" + i);
        assertion.setOrgRegion("region-" + i);
        assertion.setOwnerId("what?" + i);
        assertion.setRoleTitle("role-" + i);
        assertion.setStatus(AssertionStatus.PENDING.name());
        assertion.setUpdatedInORCID(Instant.now());
        assertion.setOrcidId("orcid-" + i);
        assertion.setPutCode(String.valueOf(i));
        assertion.setCreated(Instant.now());
        assertion.setModified(Instant.now());
        assertion.setLastSyncAttempt(Instant.now().minusSeconds(10l));
        assertion.setDisambiguatedOrgId("disambiguated-id-" + i);
        assertion.setDisambiguationSource("disambiguation-source-" + i);
        return assertion;
    }

    private Optional<OrcidRecord> getDummyOrcidRecord(String email) {
        OrcidRecord record = new OrcidRecord();
        record.setCreated(Instant.now());
        record.setEmail(email);
        record.setId("id");
        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken", null, null);
        tokens.add(newToken);
        record.setTokens(tokens);
        record.setLastNotified(Instant.now());
        record.setModified(Instant.now());
        record.setOrcid("orcid-" + email);
        return Optional.of(record);
    }

}
