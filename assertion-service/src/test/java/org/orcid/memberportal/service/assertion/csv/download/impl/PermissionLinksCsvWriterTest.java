package org.orcid.memberportal.service.assertion.csv.download.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
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
        when(assertionsRepository.countByEmailAndSalesforceId(Mockito.anyString(), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(10l);
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
        String test = csvWriter.writeCsv(assertionsUserService.getLoggedInUserSalesforceId());
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

}
