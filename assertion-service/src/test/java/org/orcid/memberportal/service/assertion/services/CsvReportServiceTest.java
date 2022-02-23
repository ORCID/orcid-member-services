package org.orcid.memberportal.service.assertion.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.csv.download.impl.AssertionsForEditCsvWriter;
import org.orcid.memberportal.service.assertion.csv.download.impl.AssertionsReportCsvWriter;
import org.orcid.memberportal.service.assertion.csv.download.impl.PermissionLinksCsvWriter;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.CsvReport;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.orcid.memberportal.service.assertion.repository.CsvReportRepository;
import org.springframework.context.MessageSource;

class CsvReportServiceTest {

    @Mock
    private CsvReportRepository csvReportRepository;

    @Mock
    private UserService userService;

    @Mock
    private AssertionsReportCsvWriter assertionsReportCsvWriter;

    @Mock
    private AssertionsForEditCsvWriter assertionsForEditCsvWriter;

    @Mock
    private PermissionLinksCsvWriter permissionLinksCsvWriter;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private MailService mailService;
    
    @Mock
    private MessageSource messageSource;

    @Captor
    private ArgumentCaptor<CsvReport> csvReportCaptor;

    @InjectMocks
    private CsvReportService csvReportService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testProcessCsvReports() throws IOException {
        Mockito.when(csvReportRepository.findAllUnprocessed()).thenReturn(getUnprocessedCsvReports());
        Mockito.when(userService.getUserById(Mockito.eq("user"))).thenReturn(getDummyUser());
        Mockito.when(assertionsReportCsvWriter.writeCsv(Mockito.eq("salesforce"))).thenReturn("report");
        Mockito.when(assertionsForEditCsvWriter.writeCsv(Mockito.eq("salesforce"))).thenReturn("report");
        Mockito.when(permissionLinksCsvWriter.writeCsv(Mockito.eq("salesforce"))).thenReturn("report");
        Mockito.when(storedFileService.storeCsvReportFile(Mockito.eq("report"), Mockito.eq("file.csv"), Mockito.any(AssertionServiceUser.class)))
                .thenReturn(getDummyStoredfile());
        Mockito.when(messageSource.getMessage(Mockito.eq("email.csvReport.affiliationsForEdit.subject"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("edit subject");
        Mockito.when(messageSource.getMessage(Mockito.eq("email.csvReport.affiliationsForEdit.content"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("edit content");
        Mockito.when(messageSource.getMessage(Mockito.eq("email.csvReport.permissionLinks.subject"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("links subject");
        Mockito.when(messageSource.getMessage(Mockito.eq("email.csvReport.permissionLinks.content"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("links content");
        Mockito.when(messageSource.getMessage(Mockito.eq("email.csvReport.affiliationStatusReport.subject"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("report subject");
        Mockito.when(messageSource.getMessage(Mockito.eq("email.csvReport.affiliationStatusReport.content"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("report content");
        Mockito.doNothing().when(mailService).sendCsvReportMail(Mockito.any(File.class), Mockito.any(AssertionServiceUser.class), Mockito.anyString(), Mockito.anyString());
        Mockito.doNothing().when(storedFileService).markAsProcessed(Mockito.any(StoredFile.class));

        csvReportService.processCsvReports();

        Mockito.verify(assertionsReportCsvWriter).writeCsv(Mockito.eq("salesforce"));
        Mockito.verify(assertionsForEditCsvWriter).writeCsv(Mockito.eq("salesforce"));
        Mockito.verify(permissionLinksCsvWriter).writeCsv(Mockito.eq("salesforce"));
        Mockito.verify(mailService).sendCsvReportMail(Mockito.any(File.class), Mockito.any(AssertionServiceUser.class), Mockito.eq("edit subject"), Mockito.eq("edit content"));
        Mockito.verify(mailService).sendCsvReportMail(Mockito.any(File.class), Mockito.any(AssertionServiceUser.class), Mockito.eq("links subject"), Mockito.eq("links content"));
        Mockito.verify(mailService).sendCsvReportMail(Mockito.any(File.class), Mockito.any(AssertionServiceUser.class), Mockito.eq("report subject"), Mockito.eq("report content"));
        Mockito.verify(storedFileService, Mockito.times(3)).markAsProcessed(Mockito.any(StoredFile.class));
    }
    
    @Test
    void testProcessCsvReportsWithError() throws IOException {
        Mockito.when(csvReportRepository.findAllUnprocessed()).thenReturn(Arrays.asList(getCsvReport(CsvReport.PERMISSION_LINKS_TYPE)));
        Mockito.when(userService.getUserById(Mockito.eq("user"))).thenReturn(getDummyUser());
        Mockito.when(permissionLinksCsvWriter.writeCsv(Mockito.eq("salesforce"))).thenThrow(new IOException("some error"));

        csvReportService.processCsvReports();

        Mockito.verify(permissionLinksCsvWriter).writeCsv(Mockito.eq("salesforce"));
        Mockito.verify(mailService, Mockito.never()).sendCsvReportMail(Mockito.any(File.class), Mockito.any(AssertionServiceUser.class), Mockito.eq("links subject"), Mockito.eq("links content"));
        Mockito.verify(storedFileService, Mockito.never()).markAsProcessed(Mockito.any(StoredFile.class));
        Mockito.verify(csvReportRepository).save(csvReportCaptor.capture());
        
        CsvReport updated = csvReportCaptor.getValue();
        assertThat(updated.getError()).isNotNull();
        assertThat(updated.getError()).contains("IOException");
        assertThat(updated.getError()).contains("some error");
    }

    @Test
    void testStoreCsvReportRequest() {
        Mockito.when(csvReportRepository.save(Mockito.any(CsvReport.class))).thenReturn(new CsvReport());

        csvReportService.storeCsvReportRequest("user", "some-filename.csv", CsvReport.ASSERTIONS_REPORT_TYPE);

        Mockito.verify(csvReportRepository).save(csvReportCaptor.capture());
        CsvReport stored = csvReportCaptor.getValue();
        assertThat(stored.getDateRequested()).isNotNull();
        assertThat(stored.getOwnerId()).isEqualTo("user");
        assertThat(stored.getStatus()).isEqualTo(CsvReport.UNPROCESSED_STATUS);
        assertThat(stored.getOriginalFilename()).isEqualTo("some-filename.csv");
        assertThat(stored.getReportType()).isEqualTo(CsvReport.ASSERTIONS_REPORT_TYPE);
    }

    private List<CsvReport> getUnprocessedCsvReports() {
        return Arrays.asList(getCsvReport(CsvReport.PERMISSION_LINKS_TYPE), getCsvReport(CsvReport.ASSERTIONS_REPORT_TYPE),
                getCsvReport(CsvReport.ASSERTIONS_FOR_EDIT_TYPE));
    }

    private CsvReport getCsvReport(String type) {
        CsvReport csvReport = new CsvReport();
        csvReport.setDateRequested(Instant.now());
        csvReport.setOwnerId("user");
        csvReport.setReportType(type);
        csvReport.setStatus(CsvReport.UNPROCESSED_STATUS);
        csvReport.setOriginalFilename("file.csv");
        return csvReport;
    }

    private AssertionServiceUser getDummyUser() {
        AssertionServiceUser user = new AssertionServiceUser();
        user.setEmail("email");
        user.setSalesforceId("salesforce");
        user.setLangKey("en");
        return user;
    }

    private StoredFile getDummyStoredfile() {
        StoredFile storedFile = new StoredFile();
        storedFile.setFileLocation("needed but won't be checked");
        return storedFile;
    }
}
