package org.orcid.mp.assertion.csv.download.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.orcid.mp.assertion.csv.download.CsvDownloadWriter;
import org.orcid.mp.assertion.domain.OrcidRecord;
import org.orcid.mp.assertion.security.EncryptUtil;
import org.orcid.mp.assertion.service.OrcidRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PermissionLinksCsvWriter extends CsvDownloadWriter {

    private static final String[] HEADERS = new String[]{"email", "link"};

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Value("${application.landingPageUrl}")
    private String landingPageUrl;

    @Override
    public String writeCsv(String memberId) throws IOException {
        return super.writeCsv(HEADERS, getRows(memberId));
    }

    private List<List<String>> getRows(String memberId) {
        List<List<String>> rows = new ArrayList<>();
        List<OrcidRecord> records = orcidRecordService.getRecordsWithoutTokens(memberId);

        for (OrcidRecord record : records) {
            String email = record.getEmail();
            long count = assertionsRepository.countByEmailAndMemberId(email, memberId);
            if (count > 0) {
                String encrypted = encryptUtil.encrypt(memberId + "&&" + email);
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