package org.orcid.user.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.orcid.user.security.AuthoritiesConstants;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.upload.UserUpload;
import org.orcid.user.upload.UserUploadReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserCsvReader implements UserUploadReader {

	private static final Logger LOG = LoggerFactory.getLogger(UserCsvReader.class);

	@Override
	public UserUpload readUsersUpload(InputStream inputStream, String createdBy) {
		InputStreamReader isr = new InputStreamReader(inputStream);
		UserUpload upload = new UserUpload();
		Iterable<CSVRecord> elements = null;
		Instant now = Instant.now();
		
		try {
			elements = CSVFormat.DEFAULT.withHeader().parse(isr);
		} catch (IOException e) {
			try {
				isr.close();
			} catch (IOException io) {
				LOG.error("Error closing csv assertions upload input stream", e);
				throw new RuntimeException(io);
			}
			
			LOG.error("Error reading CSV upload", e);
			throw new RuntimeException(e);
		}

		try {
			for (CSVRecord record : elements) {
				try {
					addUsersToUpload(record, upload, now, createdBy);
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

	private void addUsersToUpload(CSVRecord element, UserUpload upload, Instant now, String createdBy) {
		long index = element.getRecordNumber();
		String errorString = new String();
		try {
			// Validate for errors
			if (!validate(element, errorString)) {
				upload.addError(index, errorString);
			} else {
				UserDTO userDTO = getUserDTO(element, now, createdBy);
				upload.getUserDTOs().add(userDTO);
			}
		} catch (Exception e) {
			Throwable t = e.getCause();
			LOG.error("Error on line " + index, t != null ? t : e);
			upload.addError(index, t != null ? t.getMessage() : e.getMessage());
		}
	}

	private UserDTO getUserDTO(CSVRecord record, Instant now, String createdBy) {
		UserDTO u = new UserDTO();
		u.setLogin(record.get("email"));
		u.setFirstName(record.get("firstName"));
		u.setLastName(record.get("lastName"));
		u.setSalesforceId(record.get("salesforceId"));
		u.setPassword(RandomStringUtils.randomAlphanumeric(10));
		u.setSalesforceId(record.get("salesforceId"));
		u.setCreatedBy(createdBy);
		u.setCreatedDate(now);
		u.setLastModifiedBy(createdBy);
		u.setLastModifiedDate(now);
		u.setEmail(record.get("email"));
		u.setLangKey("en");
		List<String> authorities = new ArrayList<String>();
		String grants = record.get("grant");
		if (!StringUtils.isBlank(grants)) {
			if (!(grants.startsWith("[") && grants.endsWith("]"))) {
				throw new IllegalArgumentException("Grant list should start with '[' and ends with ']'");
			}
			authorities = Arrays.stream(grants.replace("[", "").replace("]", "").split(","))
					.collect(Collectors.toList());
		}
		if (authorities.contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)) {
			u.setAssertionServiceEnabled(true);
		} else {
			u.setAssertionServiceEnabled(false);
		}
		return u;
	}

	private boolean validate(CSVRecord record, String error) {
		boolean isOk = true;
		if (StringUtils.isBlank(record.get("email"))) {
			isOk = false;
			error = "Login should not be empty";
		}
		if (StringUtils.isBlank(record.get("salesforceId"))) {
			if (!isOk) {
				error += ", ";
			}
			isOk = false;
			error += "Salesforce Id should not be empty";
		}
		return isOk;
	}

}
