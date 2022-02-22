package org.orcid.memberportal.service.assertion.csv.download.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.csv.download.CsvDownloadWriter;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.security.EncryptUtil;
import org.orcid.memberportal.service.assertion.services.OrcidRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PermissionLinksCsvWriter extends CsvDownloadWriter {
    
    private static final String[] HEADERS = new String[] { "email", "link" };

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Override
    public String writeCsv(String salesforceId) throws IOException {
        return super.writeCsv(HEADERS, getRows(salesforceId));
    }
    
    private List<List<String>> getRows(String salesforceId) {
        List<List<String>> rows = new ArrayList<>();
        String landingPageUrl = applicationProperties.getLandingPageUrl();
        List<OrcidRecord> records = orcidRecordService.getRecordsWithoutTokens(salesforceId);

        for (OrcidRecord record : records) {
            String email = record.getEmail();
            List<Assertion> assertions = assertionsRepository.findByEmailAndSalesforceId(email, salesforceId);
            if (assertions.size() > 0) {
                String encrypted = encryptUtil.encrypt(salesforceId + "&&" + email);
                String link = landingPageUrl + "?state=" + encrypted;

                List<String> row = new ArrayList<>();
                row.add(email);
                row.add(link);
                rows.add(row);
            }
        }
        return rows;
    }
    
}
