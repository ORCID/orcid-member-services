package org.orcid.memberportal.service.assertion.csv.download.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.orcid.memberportal.service.assertion.csv.download.CsvDownloadWriter;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.domain.utils.AssertionUtils;
import org.orcid.memberportal.service.assertion.services.OrcidRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class AssertionsReportCsvWriter extends CsvDownloadWriter {

    private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified", "deletedFromORCID");

    private static final String[] HEADERS = new String[] { "email", "orcid", "status", "putCode", "created", "modified", "affiliation-section", "department-name",
            "role-title", "start-date", "end-date", "org-name", "org-country", "org-city", "org-region", "disambiguated-organization-identifier", "disambiguation-source",
            "external-id", "external-id-type", "external-id-url" };

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Override
    public String writeCsv(String salesforceId) throws IOException {
        List<Assertion> assertions = assertionsRepository.findBySalesforceId(salesforceId, this.SORT);
        return super.writeCsv(HEADERS, getRows(assertions, salesforceId));
    }

    private List<List<String>> getRows(List<Assertion> assertions, String salesforceId) {
        Map<String, OrcidRecord> orcidRecordMap = new HashMap<>();
        List<List<String>> rows = new ArrayList<>();
        for (Assertion a : assertions) {
            List<String> row = new ArrayList<String>();
            row.add(a.getEmail());

            if (!orcidRecordMap.containsKey(a.getEmail())) {
                OrcidRecord orcidRecord = orcidRecordService.findOneByEmail(a.getEmail())
                        .orElseThrow(() -> new IllegalArgumentException("Unable to find userInfo for " + a.getEmail()));
                orcidRecordMap.put(a.getEmail(), orcidRecord);
            }
            String orcidId = null;
            if (!StringUtils.isBlank(orcidRecordMap.get(a.getEmail()).getToken(salesforceId))) {
                orcidId = orcidRecordMap.get(a.getEmail()).getOrcid();
            }
            row.add(orcidId == null ? "" : orcidId);
            String status = AssertionUtils.getAssertionStatus(a, orcidRecordMap.get(a.getEmail()));
            String prettyStatus = AssertionStatus.valueOf(status).getValue();
            row.add(prettyStatus);
            row.add(a.getPutCode() == null ? "" : a.getPutCode());
            row.add(a.getCreated() == null ? "" : a.getCreated().toString());
            row.add(a.getModified() == null ? "" : a.getModified().toString());
            row.add(a.getAffiliationSection() == null ? "" : a.getAffiliationSection().name());
            row.add(a.getDepartmentName() == null ? "" : a.getDepartmentName());
            row.add(a.getRoleTitle() == null ? "" : a.getRoleTitle());
            row.add(getDateString(a.getStartYear(), a.getStartMonth(), a.getStartDay()));
            row.add(getDateString(a.getEndYear(), a.getEndMonth(), a.getEndDay()));
            row.add(StringUtils.isBlank(a.getOrgName()) ? "" : a.getOrgName());
            row.add(StringUtils.isBlank(a.getOrgCountry()) ? "" : a.getOrgCountry());
            row.add(StringUtils.isBlank(a.getOrgCity()) ? "" : a.getOrgCity());
            row.add(StringUtils.isBlank(a.getOrgRegion()) ? "" : a.getOrgRegion());
            row.add(StringUtils.isBlank(a.getDisambiguatedOrgId()) ? "" : a.getDisambiguatedOrgId());
            row.add(StringUtils.isBlank(a.getDisambiguationSource()) ? "" : a.getDisambiguationSource());
            row.add(StringUtils.isBlank(a.getExternalId()) ? "" : a.getExternalId());
            row.add(StringUtils.isBlank(a.getExternalIdType()) ? "" : a.getExternalIdType());
            row.add(StringUtils.isBlank(a.getExternalIdUrl()) ? "" : a.getExternalIdUrl());
            rows.add(row);
        }
        return rows;
    }

}
