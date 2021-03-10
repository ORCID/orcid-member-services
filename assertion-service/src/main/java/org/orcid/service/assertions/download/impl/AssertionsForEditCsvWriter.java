package org.orcid.service.assertions.download.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.orcid.domain.Assertion;
import org.orcid.repository.AssertionsRepository;
import org.orcid.service.UserService;
import org.orcid.service.assertions.download.CsvWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class AssertionsForEditCsvWriter extends CsvWriter {

	private final Sort SORT = new Sort(Sort.Direction.ASC, "email", "status", "created", "modified",
			"deletedFromORCID");

	@Autowired
	private UserService assertionsUserService;

	@Autowired
	private AssertionsRepository assertionsRepository;

	@Override
	public String writeCsv() throws IOException {
		String salesforceId = assertionsUserService.getLoggedInUserSalesforceId();
		List<Assertion> assertions = assertionsRepository.findBySalesforceId(salesforceId, this.SORT);

		StringBuffer buffer = new StringBuffer();
		CSVPrinter csvPrinter = getCSVPrinterWithHeaders(buffer);

		for (Assertion a : assertions) {
			List<String> values = getValues(a);
			csvPrinter.printRecord(values);
			csvPrinter.flush();
		}
		csvPrinter.close();
		return buffer.toString();
	}

	private List<String> getValues(Assertion a) {
		List<String> values = new ArrayList<String>();
		values.add(a.getEmail());
		values.add(a.getAffiliationSection() == null ? "" : a.getAffiliationSection().name());
		values.add(a.getDepartmentName() == null ? "" : a.getDepartmentName());
		values.add(a.getRoleTitle() == null ? "" : a.getRoleTitle());
		values.add(getDateString(a.getStartYear(), a.getStartMonth(), a.getStartDay()));
		values.add(getDateString(a.getEndYear(), a.getEndMonth(), a.getEndDay()));
		values.add(StringUtils.isBlank(a.getOrgName()) ? "" : a.getOrgName());
		values.add(StringUtils.isBlank(a.getOrgCountry()) ? "" : a.getOrgCountry());
		values.add(StringUtils.isBlank(a.getOrgCity()) ? "" : a.getOrgCity());
		values.add(StringUtils.isBlank(a.getOrgRegion()) ? "" : a.getOrgRegion());
		values.add(StringUtils.isBlank(a.getDisambiguationSource()) ? "" : a.getDisambiguationSource());
		values.add(StringUtils.isBlank(a.getDisambiguatedOrgId()) ? "" : a.getDisambiguatedOrgId());
		values.add(StringUtils.isBlank(a.getExternalId()) ? "" : a.getExternalId());
		values.add(StringUtils.isBlank(a.getExternalIdType()) ? "" : a.getExternalIdType());
		values.add(StringUtils.isBlank(a.getExternalIdUrl()) ? "" : a.getExternalIdUrl());
		values.add(StringUtils.isBlank(a.getUrl()) ? "" : a.getUrl());
		values.add(a.getId());
		return values;
	}
	
	private CSVPrinter getCSVPrinterWithHeaders(StringBuffer buffer) throws IOException {
		return new CSVPrinter(buffer,
				CSVFormat.DEFAULT.withHeader("email", "affiliation-section", "department-name", "role-title",
						"start-date", "end-date", "org-name", "org-country", "org-city", "org-region",
						"disambiguation-source", "disambiguated-organization-identifier", "external-id",
						"external-id-type", "external-id-url", "url", "id"));
	}

}
