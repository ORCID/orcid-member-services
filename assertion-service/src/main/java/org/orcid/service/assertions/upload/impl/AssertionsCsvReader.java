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

	private Assertion parseLine(CSVRecord line, AssertionsUpload assertionsUpload) {
		Assertion a = new Assertion();
		String id = getOptionalNullableValue(line, "id");

		if (id != null) {
			if (!assertionsService.assertionExists(id)) {
				assertionsUpload.addError(line.getRecordNumber(), "id does not exist");
				return a;
			} else {
				a.setId(id);
			}
		}

		if (deletionLine(line)) {
			return a;
		}

		String email = getOptionalNullableValue(line, "email");
		if (email == null) {
			assertionsUpload.addError(line.getRecordNumber(), "email must not be null");
			return a;
		} else {
			// attempt to change email?
			if (id != null && assertionsService.assertionExists(id)) {
				Assertion existingAssertion = assertionsService.findById(id);
				if (!email.equals(existingAssertion.getEmail())) {
					assertionsUpload.addError(line.getRecordNumber(), "affiliation email cannot be changed");
				}
			}
			a.setEmail(email);
		}

		if (getOptionalNullableValue(line, "affiliation-section") == null) {
			assertionsUpload.addError(line.getRecordNumber(), "affiliation-section must not be null");
			return a;
		} else {
			AffiliationSection affiliationSection;
			if ("INVITED-POSITION".equals(line.get("affiliation-section").toUpperCase())) {
				affiliationSection = AffiliationSection.INVITED_POSITION;
			} else {
				affiliationSection = AffiliationSection.valueOf(line.get("affiliation-section").toUpperCase());
			}
			a.setAffiliationSection(affiliationSection);
		}

		a.setDepartmentName(getOptionalNullableValue(line, "department-name"));
		a.setRoleTitle(getOptionalNullableValue(line, "role-title"));

		StringBuffer startDateBuffer = new StringBuffer();
		StringBuffer endDateBuffer = new StringBuffer();
		// Dates follows the format yyyy-MM-dd
		if (getOptionalNullableValue(line, "start-date") != null) {
			String startDate = line.get("start-date").trim();
			if (!StringUtils.isBlank(startDate)) {
				String[] startDateParts = startDate.split("-|/|\\s");
				String day = startDateParts.length > 2 ? startDateParts[2] : "0";
				if (validDate(startDate, startDateParts[0], day, line, assertionsUpload)) {
					a.setStartYear(startDateParts[0]);
					startDateBuffer.append(startDateParts[0]);
					if (startDateParts.length > 1) {
						a.setStartMonth(startDateParts[1]);
						startDateBuffer.append("-");
						startDateBuffer.append(startDateParts[1]);
					}

					if (startDateParts.length > 2) {
						a.setStartDay(startDateParts[2]);
						startDateBuffer.append("-");
						startDateBuffer.append(startDateParts[2]);
					}
				} else {
					return a;
				}
			}
		}

		// Dates follows the format yyyy-MM-dd
		if (getOptionalNullableValue(line, "end-date") != null) {
			String endDate = line.get("end-date").trim();
			if (!StringUtils.isBlank(endDate)) {
				String endDateParts[] = endDate.split("-|/|\\s");
				String day = endDateParts.length > 2 ? endDateParts[2] : "0";
				if (validDate(endDate, endDateParts[0], day, line, assertionsUpload)) {
					a.setEndYear(endDateParts[0]);
					endDateBuffer.append(endDateParts[0]);

					if (endDateParts.length > 1) {
						a.setEndMonth(endDateParts[1]);
						endDateBuffer.append("-");
						endDateBuffer.append(endDateParts[1]);
					}

					if (endDateParts.length > 2) {
						a.setEndDay(endDateParts[2]);
						endDateBuffer.append("-");
						endDateBuffer.append(endDateParts[2]);
					}

					if (startDateBuffer.length() != 0 && endDateBuffer.length() != 0) {
						if (!validStartDateEndDate(startDateBuffer.toString(), endDateBuffer.toString())) {
							assertionsUpload.addError(line.getRecordNumber(),
									"The start date cannot be greater than the end date.");
							return a;
						}
					}
				} else {
					return a;
				}
			}
		}

		if (getOptionalNullableValue(line, "org-name") == null) {
			assertionsUpload.addError(line.getRecordNumber(), "org-name must not be null");
			return a;
		} else {
			a.setOrgName(line.get("org-name"));
		}

		if (getOptionalNullableValue(line, "org-country") == null) {
			assertionsUpload.addError(line.getRecordNumber(), "org-country must not be null");
			return a;
		} else {
			try {
				Iso3166Country.valueOf(line.get("org-country"));
				a.setOrgCountry(line.get("org-country"));
			} catch (Exception e) {
				assertionsUpload.addError(line.getRecordNumber(), "Invalid org-country provided: "
						+ line.get("org-country") + " it should be one from the Iso3166Country enum");
				return a;
			}
		}

		if (getOptionalNullableValue(line, "org-city") == null) {
			assertionsUpload.addError(line.getRecordNumber(), "org-city must not be null");
			return a;
		} else {
			a.setOrgCity(line.get("org-city"));
		}

		if (getOptionalNullableValue(line, "org-region") != null) {
			a.setOrgRegion(line.get("org-region"));
		}

		if (getOptionalNullableValue(line, "disambiguation-source") == null) {
			assertionsUpload.addError(line.getRecordNumber(), "disambiguation-source-identifier must not be null");
			return a;
		} else {
			a.setDisambiguationSource(getMandatoryNullableValue(line, "disambiguation-source").toUpperCase());
		}

		if (getOptionalNullableValue(line, "disambiguated-organization-identifier") == null) {
			assertionsUpload.addError(line.getRecordNumber(), "disambiguated-organization-identifier must not be null");
			return a;
		} else {
			String orgId = AssertionUtils.stripGridURL(line.get("disambiguated-organization-identifier"));

			if (validateDisambiguatedOrganizationId(orgId, line.get("disambiguation-source"))) {
				a.setDisambiguatedOrgId(orgId);
			} else {
				assertionsUpload.addError(line.getRecordNumber(),
						"disambiguated-organization-identifier not valid. If the source is GRID must start with \"grid.\", if the source is RINGGOLD has to be a number. ");
				return a;
			}
		}

		a.setExternalId(getOptionalNullableValue(line, "external-id"));
		a.setExternalIdType(getOptionalNullableValue(line, "external-id-type"));
		a.setExternalIdUrl(getOptionalNullableValue(line, "external-id-url"));

		if (getOptionalNullableValue(line, "url") != null && !StringUtils.isBlank(line.get("url"))) {
			String url = validateUrl(line.get("url"));
			a.setUrl(url);
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
		return line.get(name);
	}

	private String getOptionalNullableValue(CSVRecord line, String name) {
		try {
			if (StringUtils.isBlank(line.get(name))) {
				return null;
			}
			return line.get(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private String validateUrl(String url) {
		if (!StringUtils.isBlank(url)) {
			url = url.trim();
			boolean valid = false;
			try {
				url = encodeUrl(url);
				valid = urlValidator.isValid(url);
			} catch (Exception e) {
			}

			if (!valid) {
				throw new IllegalArgumentException("url is invalid");
			}
		}
		return url;
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

	protected boolean validDate(String date, String year, String day, CSVRecord line,
			AssertionsUpload assertionsUpload) {

		for (DateTimeFormatter formatter : formatters) {
			try {
				LocalDate localDate = LocalDate.parse(date, formatter);
				if (isEmpty(year) || localDate.getYear() == Integer.parseInt(year)) {
					if (Integer.parseInt(day) > 0) {
						if (Integer.parseInt(day) == localDate.getDayOfMonth()) {
							return true;
						} else {
							assertionsUpload.addError(line.getRecordNumber(), "Invalid date.");
							return false;
						}
					} else {
						return true;
					}
				}
			} catch (DateTimeParseException e) {
			}
		}
		assertionsUpload.addError(line.getRecordNumber(),
				"Invalid date Format. The accepted formats are 'yyyy', 'yyyy-MM' and 'yyyy-MM-dd'");
		return false;
	}

	public boolean validStartDateEndDate(String startDate, String endDate) {

		for (DateTimeFormatter formatter : formatters) {
			try {
				LocalDate localStartDate = LocalDate.parse(startDate, formatter);
				LocalDate localEndDate = LocalDate.parse(endDate, formatter);
				if (localStartDate.isAfter(localEndDate)) {
					return false;
				}
			} catch (DateTimeParseException e) {
			}
		}

		return true;

	}

	public static boolean isEmpty(String string) {
		if (string == null || string.trim().isEmpty())
			return true;
		return false;
	}
}
