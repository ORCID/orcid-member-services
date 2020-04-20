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
import org.orcid.user.domain.UserSettings;
import org.orcid.user.security.AuthoritiesConstants;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.upload.UsersUpload;
import org.orcid.user.upload.UsersUploadReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UsersCsvReader implements UsersUploadReader {

	private static final Logger LOG = LoggerFactory.getLogger(UsersCsvReader.class);

	@Override
	public UsersUpload readUsersUpload(InputStream inputStream, String createdBy) {
		InputStreamReader isr = new InputStreamReader(inputStream);
		UsersUpload upload = new UsersUpload();
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

	private void addUsersToUpload(CSVRecord element, UsersUpload upload, Instant now, String createdBy) {
		long index = element.getRecordNumber();
		String errorString = new String();
		try {
			// Validate for errors
			if (!validate(element, errorString)) {
				upload.addError(index, errorString);
			} else {
				String salesforceId = element.get("salesforceId");
				UserDTO userDTO = getUserDTO(element);
				UserSettings userSettings = getUserSettings(userDTO.getId(), salesforceId, now, createdBy);
				upload.getUserDTOs().add(userDTO);
				upload.getUserSettings().add(userSettings);
			}
		} catch (Exception e) {
			Throwable t = e.getCause();
			LOG.error("Error on line " + index, t != null ? t : e);
			upload.addError(index, t != null ? t.getMessage() : e.getMessage());
		}
	}

	private UserSettings getUserSettings(String jhiUserId, String salesforceId, Instant now, String createdBy) {
		UserSettings us = new UserSettings();
		us.setJhiUserId(jhiUserId);
		us.setMainContact(false);
		us.setSalesforceId(salesforceId);
		us.setCreatedBy(createdBy);
		us.setCreatedDate(now);
		us.setLastModifiedBy(createdBy);
		us.setLastModifiedDate(now);
		return us;
	}

	private UserDTO getUserDTO(CSVRecord record) {
		UserDTO u = new UserDTO();
		u.setLogin(record.get("email"));
		u.setFirstName(record.get("firstName"));
		u.setLastName(record.get("lastName"));
		u.setSalesforceId(record.get("salesforceId"));
		u.setPassword(RandomStringUtils.randomAlphanumeric(10));
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
			u.setAssertionServicesEnabled(true);
		} else {
			u.setAssertionServicesEnabled(false);
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
