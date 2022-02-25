package org.orcid.memberportal.service.assertion.csv.download.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.orcid.memberportal.service.assertion.csv.download.CsvDownloadWriter;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class AssertionsForEditCsvWriter extends CsvDownloadWriter {

    private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified", "deletedFromORCID");

    private static final String[] HEADERS = new String[] { "email", "affiliation-section", "department-name", "role-title", "start-date", "end-date", "org-name",
            "org-country", "org-city", "org-region", "disambiguation-source", "disambiguated-organization-identifier", "external-id", "external-id-type",
            "external-id-url", "url", "id" };

    @Override
    public String writeCsv(String salesforceId) throws IOException {
        List<Assertion> assertions = assertionsRepository.findBySalesforceId(salesforceId, this.SORT);
        return super.writeCsv(HEADERS, getRows(assertions));
    }

    private List<List<String>> getRows(List<Assertion> assertions) {
        List<List<String>> rows = new ArrayList<>();
        for (Assertion a : assertions) {
            List<String> row = new ArrayList<String>();
            row.add(a.getEmail());
            row.add(a.getAffiliationSection() == null ? "" : a.getAffiliationSection().name());
            row.add(a.getDepartmentName() == null ? "" : a.getDepartmentName());
            row.add(a.getRoleTitle() == null ? "" : a.getRoleTitle());
            row.add(getDateString(a.getStartYear(), a.getStartMonth(), a.getStartDay()));
            row.add(getDateString(a.getEndYear(), a.getEndMonth(), a.getEndDay()));
            row.add(StringUtils.isBlank(a.getOrgName()) ? "" : a.getOrgName());
            row.add(StringUtils.isBlank(a.getOrgCountry()) ? "" : a.getOrgCountry());
            row.add(StringUtils.isBlank(a.getOrgCity()) ? "" : a.getOrgCity());
            row.add(StringUtils.isBlank(a.getOrgRegion()) ? "" : a.getOrgRegion());
            row.add(StringUtils.isBlank(a.getDisambiguationSource()) ? "" : a.getDisambiguationSource());
            row.add(StringUtils.isBlank(a.getDisambiguatedOrgId()) ? "" : a.getDisambiguatedOrgId());
            row.add(StringUtils.isBlank(a.getExternalId()) ? "" : a.getExternalId());
            row.add(StringUtils.isBlank(a.getExternalIdType()) ? "" : a.getExternalIdType());
            row.add(StringUtils.isBlank(a.getExternalIdUrl()) ? "" : a.getExternalIdUrl());
            row.add(StringUtils.isBlank(a.getUrl()) ? "" : a.getUrl());
            row.add(a.getId());
            rows.add(row);
        }
        return rows;
    }

}
