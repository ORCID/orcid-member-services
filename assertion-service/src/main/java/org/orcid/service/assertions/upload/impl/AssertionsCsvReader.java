package org.orcid.service.assertions.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.orcid.config.Constants;
import org.orcid.domain.Assertion;
import org.orcid.domain.enumeration.AffiliationSection;
import org.orcid.domain.utils.AssertionUtils;
import org.orcid.domain.validation.OrcidUrlValidator;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.service.AssertionService;
import org.orcid.service.assertions.upload.AssertionsUpload;
import org.orcid.service.assertions.upload.AssertionsUpload.AssertionsUploadDate;
import org.orcid.service.assertions.upload.AssertionsUploadReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssertionsCsvReader implements AssertionsUploadReader {

	private static final Logger LOG = LoggerFactory.getLogger(AssertionsCsvReader.class);
	private static final String GRID_STARTS_WITH = "grid.";
	private final DateTimeFormatter[] formatters = {
			new DateTimeFormatterBuilder().appendPattern("yyyy").parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
					.parseDefaulting(ChronoField.DAY_OF_MONTH, 1).toFormatter(),
			new DateTimeFormatterBuilder().appendPattern("yyyy-MM").parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
					.toFormatter(),
			new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd").parseStrict().toFormatter() };

	String[] urlValschemes = { "http", "https", "ftp" }; // DEFAULT schemes =
	// "http", "https",
	// "ftp"

	UrlValidator urlValidator = new OrcidUrlValidator(urlValschemes);

	@Autowired
	private AssertionService assertionsService;

	@Override
	public AssertionsUpload readAssertionsUpload(InputStream inputStream) throws IOException {
		AssertionsUpload upload = new AssertionsUpload();

		try (final Reader reader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
				final CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL.withHeader())) {

			for (CSVRecord record : parser) {
				try {
					Assertion assertion = parseLine(record, upload);
					if (assertion.getEmail() != null && !upload.getUsers().contains(assertion.getEmail())) {
						upload.addUser(assertion.getEmail());
					}
					upload.addAssertion(assertion);
				} catch (Exception e) {
					LOG.info("CSV upload error found for record number {}", record.getRecordNumber());
					upload.addError(record.getRecordNumber(), e.getMessage());
				}
			}
		}
		return upload;
	}

	private Assertion parseLine(CSVRecord line, AssertionsUpload upload) {
		Assertion a = new Assertion();
		a = processId(line, a, upload);

		if (deletionLine(line)) {
			return a;
		}

		a = processEmail(line, a, upload);
		a = processAffiliationSection(line, a, upload);
		a = processDepartmentName(line, a);
		a = processRoleTitle(line, a);
		a = processDates(line, upload, a);
		a = processOrgName(line, a, upload);
		a = processOrgCountry(line, a, upload);
		a = processOrgCity(line, a, upload);
		a = processOrgRegion(line, a);
		a = processDisambiguationSource(line, a, upload);
		a = processDisambiguatedOrgId(line, a, upload);
		a = processExternalId(line, a);
		a = processUrl(line, a, upload);
		return a;
	}

	private Assertion processDates(CSVRecord line, AssertionsUpload upload, Assertion a) {
		a = processStartDate(line, a, upload);
		a = processEndDate(line, a, upload);
		checkStartDateBeforeEndDate(line, a, upload);
		return a;
	}
	
	private Assertion processUrl(CSVRecord line, Assertion a, AssertionsUpload upload) {
		String url = getOptionalNullableValue(line, "url");
		if (url != null && !StringUtils.isBlank(url)) {
			url = url.trim();
			try {
				url = encodeUrl(url);
			} catch (MalformedURLException | URISyntaxException e) {
			}
			if (!urlValidator.isValid(url)) {
				upload.addError(line.getRecordNumber(), "url is invalid");
			} else {
				a.setUrl(url);
			}
		}
		return a;
	}

	private Assertion processExternalId(CSVRecord line, Assertion a) {
		a.setExternalId(getOptionalNullableValue(line, "external-id"));
		a.setExternalIdType(getOptionalNullableValue(line, "external-id-type"));
		a.setExternalIdUrl(getOptionalNullableValue(line, "external-id-url"));
		return a;
	}

	private Assertion processDisambiguatedOrgId(CSVRecord line, Assertion a, AssertionsUpload upload) {
		String orgId = getOptionalNullableValue(line, "disambiguated-organization-identifier"); 
		if (orgId == null) {
			upload.addError(line.getRecordNumber(), "disambiguated-organization-identifier must be specified");
			return a;
		} else {
			orgId = AssertionUtils.stripGridURL(orgId);

			String orgSource = getOptionalNullableValue(line, "disambiguation-source");
			if (validateDisambiguatedOrganizationId(orgId, orgSource)) {
				a.setDisambiguatedOrgId(orgId);
			} else {
				upload.addError(line.getRecordNumber(),
						"disambiguated-organization-identifier not valid. If the source is GRID must start with \"grid.\", if the source is RINGGOLD has to be a number. ");
				return a;
			}
		}
		return a;
	}

	private Assertion processDisambiguationSource(CSVRecord line, Assertion a, AssertionsUpload upload) {
		if (getOptionalNullableValue(line, "disambiguation-source") == null) {
			upload.addError(line.getRecordNumber(), "disambiguation-source-identifier must be specified");
			return a;
		} else {
			a.setDisambiguationSource(getMandatoryNullableValue(line, "disambiguation-source").toUpperCase());
		}
		return a;
	}

	private Assertion processOrgRegion(CSVRecord line, Assertion a) {
		a.setOrgRegion(getOptionalNullableValue(line, "org-region"));
		return a;
	}

	private Assertion processOrgCity(CSVRecord line, Assertion a, AssertionsUpload upload) {
		String orgCity = getOptionalNullableValue(line, "org-city"); 
		if (orgCity == null) {
			upload.addError(line.getRecordNumber(), "org-city must be specified");
			return a;
		} else {
			a.setOrgCity(orgCity);
		}
		return a;
	}

	private Assertion processOrgCountry(CSVRecord line, Assertion a, AssertionsUpload upload) {
		String orgCountry = getOptionalNullableValue(line, "org-country");
		if (orgCountry == null) {
			upload.addError(line.getRecordNumber(), "org-country must be specified");
			return a;
		} else {
			try {
				Iso3166Country.valueOf(orgCountry);
				a.setOrgCountry(orgCountry);
			} catch (Exception e) {
				upload.addError(line.getRecordNumber(), "Invalid org-country provided: " + orgCountry
						+ " it should be one from the Iso3166Country enum");
				return a;
			}
		}
		return a;
	}

	private Assertion processOrgName(CSVRecord line, Assertion a, AssertionsUpload upload) {
		String orgName = getOptionalNullableValue(line, "org-name");
		if (orgName == null) {
			upload.addError(line.getRecordNumber(), "org-name must be specified");
			return a;
		} else {
			a.setOrgName(orgName);
		}
		return a;
	}

	private Assertion processEndDate(CSVRecord line, Assertion a, AssertionsUpload upload) {
		AssertionsUpload.AssertionsUploadDate endDate = getDate(line, upload, "end-date");
		if (endDate != null) {
			a.setEndDay(endDate.getDay());
			a.setEndMonth(endDate.getMonth());
			a.setEndYear(endDate.getYear());
		}
		return a;
	}

	private Assertion processStartDate(CSVRecord line, Assertion a, AssertionsUpload upload) {
		AssertionsUpload.AssertionsUploadDate startDate = getDate(line, upload, "start-date");
		if (startDate != null) {
			a.setStartDay(startDate.getDay());
			a.setStartMonth(startDate.getMonth());
			a.setStartYear(startDate.getYear());
		}
		return a;
	}

	private AssertionsUpload.AssertionsUploadDate getDate(CSVRecord line, AssertionsUpload upload, String elementName) {
		String year = null;
		String month = null;
		String day = null;
		
		// Dates follows the format yyyy-MM-dd
		String date = getOptionalNullableValue(line, elementName);
		if (date != null && !StringUtils.isBlank(date)) {
			String[] dateParts = date.split("-|/|\\s");
			String yearToValidate = dateParts[0];
			String dayToValidate = dateParts.length > 2 ? dateParts[2] : "0";
			if (validDate(date, yearToValidate, dayToValidate, line, upload)) {
				year = dateParts[0];
				if (dateParts.length > 1) {
					month = dateParts[1];
				}

				if (dateParts.length > 2) {
					day = dateParts[2];
				}
				return new AssertionsUpload.AssertionsUploadDate(year, month, day);
			}
		}
		return null;
	}

	private Assertion processRoleTitle(CSVRecord line, Assertion a) {
		a.setRoleTitle(getOptionalNullableValue(line, "role-title"));
		return a;
	}

	private Assertion processDepartmentName(CSVRecord line, Assertion a) {
		a.setDepartmentName(getOptionalNullableValue(line, "department-name"));
		return a;
	}

	private Assertion processAffiliationSection(CSVRecord line, Assertion a, AssertionsUpload upload) {
		String affiliationSectionValue = getOptionalNullableValue(line, "affiliation-section");
		if (affiliationSectionValue == null || affiliationSectionValue.isEmpty()) {
			upload.addError(line.getRecordNumber(), "affiliation-section must be specified");
			return a;
		} else {
			AffiliationSection affiliationSection;
			if ("INVITED-POSITION".equals(affiliationSectionValue.toUpperCase())) {
				affiliationSection = AffiliationSection.INVITED_POSITION;
			} else {
				affiliationSection = AffiliationSection.valueOf(affiliationSectionValue.toUpperCase());
			}
			a.setAffiliationSection(affiliationSection);
		}
		return a;
	}

	private Assertion processId(CSVRecord line, Assertion a, AssertionsUpload upload) {
		String id = getOptionalNullableValue(line, "id");
		if (id != null) {
			if (!assertionsService.assertionExists(id)) {
				upload.addError(line.getRecordNumber(), "id does not exist");
				return a;
			} else {
				a.setId(id);
			}
		}
		return a;
	}

	private Assertion processEmail(CSVRecord line, Assertion a, AssertionsUpload upload) {
		String id = getOptionalNullableValue(line, "id");
		String email = getOptionalNullableValue(line, "email");
		if (email == null) {
			upload.addError(line.getRecordNumber(), "email must be specified");
			return a;
		} else {
			// attempt to change email?
			if (id != null && assertionsService.assertionExists(id)) {
				Assertion existingAssertion = assertionsService.findById(id);
				if (!email.equals(existingAssertion.getEmail())) {
					upload.addError(line.getRecordNumber(), "email cannot be changed");
				}
			}
			a.setEmail(email);
		}
		return a;
	}

	private boolean deletionLine(CSVRecord line) {
		if (getOptionalNullableValue(line, "id") == null) {
			return false;
		}
		return empty(line, "email") && empty(line, "affiliation-section") && empty(line, "department-name")
				&& empty(line, "role-title") && empty(line, "start-date") && empty(line, "end-date")
				&& empty(line, "org-name") && empty(line, "org-country") && empty(line, "org-city")
				&& empty(line, "org-region") && empty(line, "disambiguation-source")
				&& empty(line, "disambiguated-organization-identifier") && empty(line, "external-id")
				&& empty(line, "external-id-type") && empty(line, "external-id-url") && empty(line, "url");
	}

	private boolean empty(CSVRecord line, String columnName) {
		return !line.isSet(columnName) || getOptionalNullableValue(line, columnName) == null
				|| getOptionalNullableValue(line, columnName).isEmpty();
	}

	private String getMandatoryNullableValue(CSVRecord line, String name) {
		if (StringUtils.isBlank(line.get(name))) {
			return null;
		}
		return line.get(name).trim();
	}

	private String getOptionalNullableValue(CSVRecord line, String name) {
		try {
			if (StringUtils.isBlank(line.get(name))) {
				return null;
			}
			return line.get(name).trim();
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private boolean validateDisambiguatedOrganizationId(String orgId, String orgSource) {
		if (StringUtils.equalsIgnoreCase(orgSource, Constants.GRID_ORG_SOURCE)) {
			return orgId.length() > (GRID_STARTS_WITH.length() + 1)
					&& StringUtils.equals(orgId.substring(0, GRID_STARTS_WITH.length()), GRID_STARTS_WITH);
		} else if (StringUtils.equalsIgnoreCase(orgSource, Constants.RINGGOLD_ORG_SOURCE)) {
			return orgId.chars().allMatch(x -> Character.isDigit(x));
		}
		return false;
	}

	private String encodeUrl(String urlString) throws MalformedURLException, URISyntaxException {
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			// try adding protocol, which could be missing
			url = new URL("http://" + urlString);
		}
		URI encoded = new URI(url.getProtocol(), url.getHost(), url.getPath(), null);
		return encoded.toASCIIString();
	}

	protected boolean validDate(String date, String year, String day, CSVRecord line, AssertionsUpload upload) {
		for (DateTimeFormatter formatter : formatters) {
			try {
				LocalDate localDate = LocalDate.parse(date, formatter);
				if (isEmpty(year) || localDate.getYear() == Integer.parseInt(year)) {
					if (Integer.parseInt(day) > 0) {
						if (Integer.parseInt(day) == localDate.getDayOfMonth()) {
							return true;
						} else {
							upload.addError(line.getRecordNumber(), "Invalid date.");
							return false;
						}
					} else {
						return true;
					}
				}
			} catch (DateTimeParseException e) {
			}
		}
		upload.addError(line.getRecordNumber(),
				"Invalid date Format. The accepted formats are 'yyyy', 'yyyy-MM' and 'yyyy-MM-dd'");
		return false;
	}

	private void checkStartDateBeforeEndDate(CSVRecord line, Assertion a, AssertionsUpload upload) {
		AssertionsUploadDate startDate = getDate(line, upload, "start-date");
		AssertionsUploadDate endDate = getDate(line, upload, "end-date");
		
		if (startDate != null && endDate != null) {
			String startDateString = startDate.toString();
			String endDateString = endDate.toString();
			LocalDate localStartDate = null;
			LocalDate localEndDate = null;
			
			// initialise dates, which could be of different formats
			for (DateTimeFormatter formatter : formatters) {
				try {
					localStartDate = LocalDate.parse(startDateString, formatter);
				} catch (DateTimeParseException e) {
				}
				try {
					localEndDate = LocalDate.parse(endDateString, formatter);
				} catch (DateTimeParseException e) {
				}
			}

			if (localStartDate.isAfter(localEndDate)) {
				upload.addError(line.getRecordNumber(),
						"Start date cannot be after the end date.");
			}
		}
	}

	public static boolean isEmpty(String string) {
		if (string == null || string.trim().isEmpty())
			return true;
		return false;
	}
	

}
