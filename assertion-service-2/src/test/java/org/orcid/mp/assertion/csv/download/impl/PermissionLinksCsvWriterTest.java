package org.orcid.mp.assertion.csv.download.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.mp.assertion.domain.OrcidRecord;
import org.orcid.mp.assertion.repository.AssertionRepository;
import org.orcid.mp.assertion.security.EncryptUtil;
import org.orcid.mp.assertion.service.OrcidRecordService;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class PermissionLinksCsvWriterTest {

    private static final String DEFAULT_SALESFORCE_ID = "salesforce-id";

    @Mock
    private EncryptUtil encryptUtil;

    @Mock
    private OrcidRecordService orcidRecordService;

    @Mock
    private AssertionRepository assertionsRepository;

    @InjectMocks
    private PermissionLinksCsvWriter csvWriter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(csvWriter, "landingPageUrl", "https://member-portal.com");
        when(orcidRecordService.getRecordsWithoutTokens(Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(getListOfOrcidRecords());
        when(assertionsRepository.countByEmailAndSalesforceId(Mockito.anyString(), Mockito.eq(DEFAULT_SALESFORCE_ID))).thenReturn(10L);
        when(encryptUtil.encrypt(Mockito.anyString())).thenAnswer(new Answer<String>() {
            // just return unencrypted arg
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                String arg = invocation.getArgument(0);
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
        String test = csvWriter.writeCsv(DEFAULT_SALESFORCE_ID);
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
