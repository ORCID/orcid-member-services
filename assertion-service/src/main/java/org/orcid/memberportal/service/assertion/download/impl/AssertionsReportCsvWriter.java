package org.orcid.memberportal.service.assertion.download.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.orcid.memberportal.service.assertion.domain.utils.AssertionUtils;
import org.orcid.memberportal.service.assertion.download.CsvWriter;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.services.OrcidRecordService;
import org.orcid.memberportal.service.assertion.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class AssertionsReportCsvWriter extends CsvWriter {

    private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified", "deletedFromORCID");

    @Autowired
    private UserService assertionsUserService;

    @Autowired
    private AssertionRepository assertionsRepository;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Override
    public String writeCsv() throws IOException {
        String salesForceId = assertionsUserService.getLoggedInUserSalesforceId();
        List<Assertion> assertions = assertionsRepository.findBySalesforceId(salesForceId, this.SORT);
        StringBuffer buffer = new StringBuffer();
        CSVPrinter csvPrinter = getCSVPrinterWithHeaders(buffer);
        Map<String, OrcidRecord> orcidRecordMap = new HashMap<>();

        for (Assertion a : assertions) {
            List<String> values = getValues(a, orcidRecordMap, salesForceId);
            csvPrinter.printRecord(values);
            csvPrinter.flush();
        }
        csvPrinter.close();
        return buffer.toString();
    }

    private List<String> getValues(Assertion a, Map<String, OrcidRecord> orcidRecordMap, String salesforceId) {
        List<String> values = new ArrayList<String>();
        values.add(a.getEmail());

        if (!orcidRecordMap.containsKey(a.getEmail())) {
            OrcidRecord orcidRecord = orcidRecordService.findOneByEmail(a.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Unable to find userInfo for " + a.getEmail()));
            orcidRecordMap.put(a.getEmail(), orcidRecord);
        }
        String orcidId = null;
        if (!StringUtils.isBlank(orcidRecordMap.get(a.getEmail()).getToken(salesforceId))) {
            orcidId = orcidRecordMap.get(a.getEmail()).getOrcid();
        }
        values.add(orcidId == null ? "" : orcidId);
        values.add(AssertionUtils.getAssertionStatus(a, orcidRecordMap.get(a.getEmail())));
        values.add(a.getPutCode() == null ? "" : a.getPutCode());
        values.add(a.getCreated() == null ? "" : a.getCreated().toString());
        values.add(a.getModified() == null ? "" : a.getModified().toString());
        values.add(a.getAffiliationSection() == null ? "" : a.getAffiliationSection().name());
        values.add(a.getDepartmentName() == null ? "" : a.getDepartmentName());
        values.add(a.getRoleTitle() == null ? "" : a.getRoleTitle());
        values.add(getDateString(a.getStartYear(), a.getStartMonth(), a.getStartDay()));
        values.add(getDateString(a.getEndYear(), a.getEndMonth(), a.getEndDay()));
        values.add(StringUtils.isBlank(a.getOrgName()) ? "" : a.getOrgName());
        values.add(StringUtils.isBlank(a.getOrgCountry()) ? "" : a.getOrgCountry());
        values.add(StringUtils.isBlank(a.getOrgCity()) ? "" : a.getOrgCity());
        values.add(StringUtils.isBlank(a.getOrgRegion()) ? "" : a.getOrgRegion());
        values.add(StringUtils.isBlank(a.getDisambiguatedOrgId()) ? "" : a.getDisambiguatedOrgId());
        values.add(StringUtils.isBlank(a.getDisambiguationSource()) ? "" : a.getDisambiguationSource());
        values.add(StringUtils.isBlank(a.getExternalId()) ? "" : a.getExternalId());
        values.add(StringUtils.isBlank(a.getExternalIdType()) ? "" : a.getExternalIdType());
        values.add(StringUtils.isBlank(a.getExternalIdUrl()) ? "" : a.getExternalIdUrl());
        return values;
    }

    private CSVPrinter getCSVPrinterWithHeaders(StringBuffer buffer) throws IOException {
        return new CSVPrinter(buffer,
                CSVFormat.DEFAULT.withHeader("email", "orcid", "status", "putCode", "created", "modified", "affiliation-section", "department-name", "role-title",
                        "start-date", "end-date", "org-name", "org-country", "org-city", "org-region", "disambiguated-organization-identifier", "disambiguation-source",
                        "external-id", "external-id-type", "external-id-url"));

    }

}
