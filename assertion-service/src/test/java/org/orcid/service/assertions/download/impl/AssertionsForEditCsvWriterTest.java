package org.orcid.service.assertions.download.impl;

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
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.OrcidToken;
import org.orcid.domain.enumeration.AffiliationSection;
import org.orcid.repository.AssertionRepository;
import org.orcid.service.OrcidRecordService;
import org.orcid.service.UserService;
import org.springframework.data.domain.Sort;

public class AssertionsForEditCsvWriterTest {

    private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

    @Mock
    private AssertionRepository assertionsRepository;

    @Mock
    private UserService assertionsUserService;

    @Mock
    private OrcidRecordService orcidRecordService;

    @InjectMocks
    private AssertionsForEditCsvWriter csvWriter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(assertionsUserService.getLoggedInUserSalesforceId()).thenReturn("salesforceId");
        when(assertionsRepository.findBySalesforceId(Mockito.eq("salesforceId"), Mockito.any(Sort.class)))
                .thenReturn(getListOfAsserions());
        when(orcidRecordService.findOneByEmail(Mockito.anyString())).thenReturn(getDummyOrcidRecord());
    }

    @Test
    public void testWriteAssertionsReport() throws IOException {
        String test = csvWriter.writeCsv();
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
        assertEquals(AffiliationSection.values()[i].toString(), values[1].trim());
        assertEquals("department-" + i, values[2].trim());
        assertEquals("role-" + i, values[3].trim());
        assertEquals("2010-12-1", values[4].trim());
        assertEquals("2015-12-1", values[5].trim());
        assertEquals("org-" + i, values[6].trim());
        assertEquals("US", values[7].trim());
        assertEquals("city-" + i, values[8].trim());
        assertEquals("region-" + i, values[9].trim());
        assertEquals("source-" + i, values[10].trim());
        assertEquals("id-" + i, values[11].trim());
        assertEquals("extId-" + i, values[12].trim());
        assertEquals("extIdType-" + i, values[13].trim());
        assertEquals("extIdUrl-" + i, values[14].trim());
        assertEquals("url-" + i, values[15].trim());
        assertEquals("affiliation-id-" + i, values[16].trim());
    }

    private void checkHeaders(String[] headers) {
        assertEquals("email", headers[0].trim());
        assertEquals("affiliation-section", headers[1].trim());
        assertEquals("department-name", headers[2].trim());
        assertEquals("role-title", headers[3].trim());
        assertEquals("start-date", headers[4].trim());
        assertEquals("end-date", headers[5].trim());
        assertEquals("org-name", headers[6].trim());
        assertEquals("org-country", headers[7].trim());
        assertEquals("org-city", headers[8].trim());
        assertEquals("org-region", headers[9].trim());
        assertEquals("disambiguation-source", headers[10].trim());
        assertEquals("disambiguated-organization-identifier", headers[11].trim());
        assertEquals("external-id", headers[12].trim());
        assertEquals("external-id-type", headers[13].trim());
        assertEquals("external-id-url", headers[14].trim());
        assertEquals("url", headers[15].trim());
        assertEquals("id", headers[16].trim());
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
        assertion.setUpdated(true);
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

    private Optional<OrcidRecord> getDummyOrcidRecord() {
        OrcidRecord record = new OrcidRecord();
        record.setCreated(Instant.now());
        record.setEmail("test@test.com");
        record.setId("id");
        List<OrcidToken> tokens = new ArrayList<OrcidToken>();
        OrcidToken newToken = new OrcidToken(DEFAULT_SALESFORCE_ID, "idToken", null, null);
        tokens.add(newToken);
        record.setTokens(tokens);
        record.setLastNotified(Instant.now());
        record.setModified(Instant.now());
        record.setOrcid("orcid");
        return Optional.of(record);
    }

}
