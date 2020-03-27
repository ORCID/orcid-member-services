package org.orcid.service.assertions.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.orcid.domain.Assertion;
import org.orcid.domain.enumeration.AffiliationSection;
import org.orcid.domain.validation.OrcidUrlValidator;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.service.assertions.upload.AssertionsUpload;
import org.orcid.service.assertions.upload.AssertionsUploadReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AssertionsCsvReader implements AssertionsUploadReader {

	private static final Logger LOG = LoggerFactory.getLogger(AssertionsCsvReader.class);

	String[] urlValschemes = { "http", "https", "ftp" }; // DEFAULT schemes =
	// "http", "https",
	// "ftp"

	UrlValidator urlValidator = new OrcidUrlValidator(urlValschemes);

	@Override
	public AssertionsUpload readAssertionsUpload(InputStream inputStream) throws IOException {
		InputStreamReader isr = new InputStreamReader(inputStream);
		AssertionsUpload upload = new AssertionsUpload();
		Iterable<CSVRecord> elements = CSVFormat.DEFAULT.withHeader().parse(isr);

		try {
			for (CSVRecord record : elements) {
				try {
					Assertion assertion = parseLine(record);

					// Create the userInfo if needed
					if (!upload.getUsers().contains(assertion.getEmail())) {
						upload.addUser(assertion.getEmail());
					}
					upload.addAssertion(assertion);
				} catch (Exception e) {
					LOG.info("CSV upload error found for record number {}", record.getRecordNumber());
					upload.addError(record.getRecordNumber(), e.getMessage());
				}
			}
		} finally {
			try {
				isr.close();
			} catch (IOException e) {
				LOG.error("Error closing csv assertions upload input stream", e);
				throw new RuntimeException(e);
			}
		}

		return upload;
	}

	private Assertion parseLine(CSVRecord line) {
		Assertion a = new Assertion();
		if (StringUtils.isBlank(line.get("email"))) {
			throw new IllegalArgumentException("email must not be null");
		}
		a.setEmail(line.get("email"));

		if (StringUtils.isBlank(line.get("affiliation-section"))) {
			throw new IllegalArgumentException("affiliation-section must not be null");
		}
		a.setAffiliationSection(AffiliationSection.valueOf(line.get("affiliation-section").toUpperCase()));
		a.setDepartmentName(getMandatoryNullableValue(line, "department-name"));
		a.setRoleTitle(getMandatoryNullableValue(line, "role-title"));

		// Dates follows the format yyyy-MM-dd
		String startDate = line.get("start-date");
		if (!StringUtils.isBlank(startDate)) {
			String[] startDateParts = startDate.split("-|/|\\s");
			a.setStartYear(startDateParts[0]);
			if (startDateParts.length > 1) {
				a.setStartMonth(startDateParts[1]);
			}

			if (startDateParts.length > 2) {
				a.setStartDay(startDateParts[2]);
			}
		}

		// Dates follows the format yyyy-MM-dd
		String endDate = line.get("end-date");
		if (!StringUtils.isBlank(endDate)) {
			String endDateParts[] = endDate.split("-|/|\\s");
			a.setEndYear(endDateParts[0]);
			if (endDateParts.length > 1) {
				a.setEndMonth(endDateParts[1]);
			}

			if (endDateParts.length > 2) {
				a.setEndDay(endDateParts[2]);
			}
		}
		if (StringUtils.isBlank(line.get("org-name"))) {
			throw new IllegalArgumentException("org-name must not be null");
		}
		a.setOrgName(line.get("org-name"));
		if (StringUtils.isBlank(line.get("org-country"))) {
			throw new IllegalArgumentException("org-country must not be null");
		} else {
			try {
				Iso3166Country.valueOf(line.get("org-country"));
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid org-country provided: " + line.get("org-country")
						+ " it should be one from the Iso3166Country enum");
			}
		}
		a.setOrgCountry(line.get("org-country"));
		if (StringUtils.isBlank(line.get("org-city"))) {
			throw new IllegalArgumentException("org-city must not be null");
		}
		a.setOrgCity(line.get("org-city"));
		a.setOrgRegion(line.get("org-region"));
		if (StringUtils.isBlank(line.get("disambiguated-organization-identifier"))) {
			throw new IllegalArgumentException("disambiguated-organization-identifier must not be null");
		}
		a.setDisambiguatedOrgId(line.get("disambiguated-organization-identifier"));
		if (StringUtils.isBlank(line.get("disambiguation-source"))) {
			throw new IllegalArgumentException("disambiguation-source must not be null");
		}
		a.setDisambiguationSource(getMandatoryNullableValue(line, "disambiguation-source"));
		a.setExternalId(getOptionalMandatoryNullable(line, "external-id"));
		a.setExternalIdType(getOptionalMandatoryNullable(line, "external-id-type"));
		a.setExternalIdUrl(getOptionalMandatoryNullable(line, "external-id-url"));
		
		if (!StringUtils.isBlank(line.get("url"))) {
			String url = validateUrl(line.get("url"));
			a.setUrl(url);
		}
		
		return a;
	}

	private String getMandatoryNullableValue(CSVRecord line, String name) {
		if (StringUtils.isBlank(line.get(name))) {
			return null;
		}
		return line.get(name);
	}

	private String getOptionalMandatoryNullable(CSVRecord line, String name) {
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

}
