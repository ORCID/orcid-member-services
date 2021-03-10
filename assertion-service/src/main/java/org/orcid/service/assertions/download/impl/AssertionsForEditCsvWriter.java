package org.orcid.service.assertions.download.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.repository.AssertionsRepository;
import org.orcid.service.OrcidRecordService;
import org.orcid.service.UserService;
import org.orcid.service.assertions.download.CsvWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class AssertionsForEditCsvWriter implements CsvWriter {

	private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified",
			"deletedFromORCID");

	@Autowired
	private UserService assertionsUserService;

	@Autowired
	private AssertionsRepository assertionsRepository;

	@Autowired
	private OrcidRecordService orcidRecordService;

	@Override
	public String writeCsv() throws IOException {
		String salesforceId = assertionsUserService.getLoggedInUserSalesforceId();
		List<Assertion> assertions = assertionsRepository.findBySalesforceId(salesforceId, this.SORT);

		StringBuffer buffer = new StringBuffer();
		CSVPrinter csvPrinter = new CSVPrinter(buffer,
				CSVFormat.DEFAULT.withHeader("email", "affiliation-section", "department-name", "role-title",
						"start-date", "end-date", "org-name", "org-country", "org-city", "org-region",
						"disambiguation-source", "disambiguated-organization-identifier", "external-id",
						"external-id-type", "external-id-url", "url", "id"));
		Map<String, OrcidRecord> orcidRecordMap = new HashMap<>();
		List<String> elements;
		for (Assertion a : assertions) {
			elements = new ArrayList<String>();
			elements.add(a.getEmail());

			if (!orcidRecordMap.containsKey(a.getEmail())) {
				OrcidRecord orcidRecord = orcidRecordService.findOneByEmail(a.getEmail())
						.orElseThrow(() -> new IllegalArgumentException("Unable to find userInfo for " + a.getEmail()));
				orcidRecordMap.put(a.getEmail(), orcidRecord);
			}
			elements.add(a.getAffiliationSection() == null ? "" : a.getAffiliationSection().name());
			elements.add(a.getDepartmentName() == null ? "" : a.getDepartmentName());
			elements.add(a.getRoleTitle() == null ? "" : a.getRoleTitle());

			if (!StringUtils.isBlank(a.getStartYear())) {
				String startDate = a.getStartYear();
				if (!StringUtils.isBlank(a.getStartMonth())) {
					startDate += '-' + a.getStartMonth();
					if (!StringUtils.isBlank(a.getStartDay())) {
						startDate += '-' + a.getStartDay();
					}
				}
				elements.add(startDate);
			} else {
				elements.add(StringUtils.EMPTY);
			}

			if (!StringUtils.isBlank(a.getEndYear())) {
				String endDate = a.getEndYear();
				if (!StringUtils.isBlank(a.getEndMonth())) {
					endDate += '-' + a.getEndMonth();
					if (!StringUtils.isBlank(a.getEndDay())) {
						endDate += '-' + a.getEndDay();
					}
				}
				elements.add(endDate);
			} else {
				elements.add(StringUtils.EMPTY);
			}

			elements.add(StringUtils.isBlank(a.getOrgName()) ? "" : a.getOrgName());
			elements.add(StringUtils.isBlank(a.getOrgCountry()) ? "" : a.getOrgCountry());
			elements.add(StringUtils.isBlank(a.getOrgCity()) ? "" : a.getOrgCity());
			elements.add(StringUtils.isBlank(a.getOrgRegion()) ? "" : a.getOrgRegion());
			elements.add(StringUtils.isBlank(a.getDisambiguationSource()) ? "" : a.getDisambiguationSource());
			elements.add(StringUtils.isBlank(a.getDisambiguatedOrgId()) ? "" : a.getDisambiguatedOrgId());
			elements.add(StringUtils.isBlank(a.getExternalId()) ? "" : a.getExternalId());
			elements.add(StringUtils.isBlank(a.getExternalIdType()) ? "" : a.getExternalIdType());
			elements.add(StringUtils.isBlank(a.getExternalIdUrl()) ? "" : a.getExternalIdUrl());
			elements.add(StringUtils.isBlank(a.getUrl()) ? "" : a.getUrl());
			elements.add(a.getId());
			csvPrinter.printRecord(elements);
			csvPrinter.flush();
		}
		csvPrinter.close();
		return buffer.toString();
	}

}
