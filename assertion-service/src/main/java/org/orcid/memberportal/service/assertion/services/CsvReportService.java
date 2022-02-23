package org.orcid.memberportal.service.assertion.services;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.orcid.memberportal.service.assertion.csv.download.impl.AssertionsForEditCsvWriter;
import org.orcid.memberportal.service.assertion.csv.download.impl.AssertionsReportCsvWriter;
import org.orcid.memberportal.service.assertion.csv.download.impl.PermissionLinksCsvWriter;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceUser;
import org.orcid.memberportal.service.assertion.domain.CsvReport;
import org.orcid.memberportal.service.assertion.domain.StoredFile;
import org.orcid.memberportal.service.assertion.repository.CsvReportRepository;
import org.orcid.memberportal.service.assertion.services.locale.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class CsvReportService {

    private static final Logger LOG = LoggerFactory.getLogger(CsvReportService.class);

    static final String MEMBER_ASSERTION_STATS_FILE_TYPE = "assertion-stats";

    static final String ASSERTIONS_CSV_FILE_TYPE = "assertions-csv";

    static final String CSV_REPORT_FILE_TYPE = "csv-report";

    @Autowired
    private CsvReportRepository csvReportRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AssertionsReportCsvWriter assertionsReportCsvWriter;

    @Autowired
    private AssertionsForEditCsvWriter assertionsForEditCsvWriter;

    @Autowired
    private PermissionLinksCsvWriter permissionLinksCsvWriter;

    @Autowired
    private StoredFileService storedFileService;
    
    @Autowired
    private MailService mailService;
    
    @Autowired
    private MessageSource messageSource;
    
    public void storeCsvReportRequest(String userId, String filename, String type) {
        Instant now = Instant.now();
        CsvReport csvReport = new CsvReport();
        csvReport.setDateRequested(now);
        csvReport.setOwnerId(userId);
        csvReport.setReportType(type);
        csvReport.setStatus(CsvReport.UNPROCESSED_STATUS);
        csvReport.setOriginalFilename(filename);
        csvReportRepository.save(csvReport);
    }
    
    public void processCsvReports() {
        LOG.info("Processing pending CSV reports");
        List<CsvReport> reports = csvReportRepository.findAllUnprocessed();
        reports.forEach(r -> {
            try {
                processCsvReportRequest(r);
            } catch (IOException e) {
                LOG.warn("Failed to generate CSV report of type {} for user {}", r.getReportType(), r.getOwnerId(), e);
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                r.setError(sw.toString());
                r.setStatus(CsvReport.FAILURE_STATUS);
                csvReportRepository.save(r);
            }
        });
        LOG.info("CSV reports processed");
    }

    private void processCsvReportRequest(CsvReport csvReport) throws IOException {
        AssertionServiceUser user = userService.getUserById(csvReport.getOwnerId());
        String salesforceId = user.getSalesforceId();
        Locale locale = LocaleUtils.getLocale(user.getLangKey());
        
        String subject = null;
        String content = null;
        String report = null;

        LOG.info("Generating csv report of type {} for user {}", csvReport.getReportType(), user.getEmail());
        if (CsvReport.ASSERTIONS_FOR_EDIT_TYPE.equals(csvReport.getReportType())) {
            report = assertionsForEditCsvWriter.writeCsv(salesforceId);
            subject = messageSource.getMessage("email.csvReport.affiliationsForEdit.subject", null, locale);
            content = messageSource.getMessage("email.csvReport.affiliationsForEdit.content", null, locale);
        } else if (CsvReport.ASSERTIONS_REPORT_TYPE.equals(csvReport.getReportType())) {
            report = assertionsReportCsvWriter.writeCsv(salesforceId);
            subject = messageSource.getMessage("email.csvReport.affiliationStatusReport.subject", null, locale);
            content = messageSource.getMessage("email.csvReport.affiliationStatusReport.content", null, locale);
        } else if (CsvReport.PERMISSION_LINKS_TYPE.equals(csvReport.getReportType())) {
            report = permissionLinksCsvWriter.writeCsv(salesforceId);
            subject = messageSource.getMessage("email.csvReport.permissionLinks.subject", null, locale);
            content = messageSource.getMessage("email.csvReport.permissionLinks.content", null, locale);
        }

        StoredFile storedFile = storedFileService.storeCsvReportFile(report, csvReport.getOriginalFilename(), user);
        csvReport.setDateGenerated(storedFile.getDateWritten());
        csvReportRepository.save(csvReport);

        LOG.info("Report generated. Sending report to {},,,", user.getEmail());
        File reportFile = new File(storedFile.getFileLocation());
        mailService.sendCsvReportMail(reportFile, user, subject, content);
        LOG.info("Report sent to {}", user.getEmail());
        
        storedFileService.markAsProcessed(storedFile);
    }

}
