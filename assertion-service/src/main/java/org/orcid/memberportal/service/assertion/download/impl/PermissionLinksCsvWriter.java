package org.orcid.memberportal.service.assertion.download.impl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.download.CsvWriter;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.security.EncryptUtil;
import org.orcid.memberportal.service.assertion.services.OrcidRecordService;
import org.orcid.memberportal.service.assertion.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PermissionLinksCsvWriter extends CsvWriter {

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private UserService assertionsUserService;

    @Autowired
    private AssertionRepository assertionsRepository;

    @Override
    public String writeCsv() throws IOException {
        String landingPageUrl = applicationProperties.getLandingPageUrl();
        StringBuffer buffer = new StringBuffer();
        CSVPrinter csvPrinter = getCSVPrinterWithHeaders(buffer);

        String salesForceId = assertionsUserService.getLoggedInUserSalesforceId();
        List<OrcidRecord> records = orcidRecordService.getRecordsWithoutTokens(salesForceId);

        for (OrcidRecord record : records) {
            String email = record.getEmail();
            List<Assertion> assertions = assertionsRepository.findByEmailAndSalesforceId(email, salesForceId);
            if (assertions.size() > 0) {
                String encrypted = encryptUtil.encrypt(salesForceId + "&&" + email);
                String link = landingPageUrl + "?state=" + encrypted;
                csvPrinter.printRecord(email, link);
            }
        }

        csvPrinter.flush();
        csvPrinter.close();
        return buffer.toString();
    }

    private CSVPrinter getCSVPrinterWithHeaders(StringBuffer buffer) throws IOException {
        return new CSVPrinter(buffer, CSVFormat.DEFAULT.withHeader("email", "link"));
    }

}
