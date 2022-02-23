package org.orcid.memberportal.service.assertion.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

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
        Mockito.doNothing().when(mailService).sendCsvReportMail(Mockito.any(File.class), Mockito.any(AssertionServiceUser.class));

        csvReportService.processCsvReports();

        Mockito.verify(assertionsReportCsvWriter).writeCsv(Mockito.eq("salesforce"));
        Mockito.verify(assertionsForEditCsvWriter).writeCsv(Mockito.eq("salesforce"));
        Mockito.verify(permissionLinksCsvWriter).writeCsv(Mockito.eq("salesforce"));
        Mockito.verify(mailService, Mockito.times(3)).sendCsvReportMail(Mockito.any(File.class), Mockito.any(AssertionServiceUser.class));
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
        return user;
    }

    private StoredFile getDummyStoredfile() {
        StoredFile storedFile = new StoredFile();
        storedFile.setFileLocation("needed but won't be checked");
        return storedFile;
    }
}
