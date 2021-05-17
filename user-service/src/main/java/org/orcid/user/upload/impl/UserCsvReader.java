package org.orcid.user.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.orcid.user.domain.User;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.service.UserService;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.upload.UserUpload;
import org.orcid.user.upload.UserUploadReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class UserCsvReader implements UserUploadReader {

	private static final Logger LOG = LoggerFactory.getLogger(UserCsvReader.class);

	private Map<String, String> orgWithOwner;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserService userService;

	@Autowired
	private MessageSource messageSource;

	private EmailValidator emailValidator = EmailValidator.getInstance(false);

	@Override
	public UserUpload readUsersUpload(InputStream inputStream, User user) throws IOException {
		UserUpload upload = new UserUpload();
		Instant now = Instant.now();
		orgWithOwner = getOrganizationsWithOwner();

		try (final Reader reader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
				final CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

			for (CSVRecord record : parser) {
				try {
					addUsersToUpload(record, upload, now, user);
				} catch (Exception e) {
					LOG.info("CSV upload error found for record number {}", record.getRecordNumber());
					upload.addError(record.getRecordNumber(), e.getMessage());
				}
			}
		}
		upload.setOrgWithOwner(this.orgWithOwner);
		return upload;
	}

	private void addUsersToUpload(CSVRecord record, UserUpload upload, Instant now, User currentUser) {
		try {
			if (valid(record, upload, currentUser)) {
				UserDTO userDTO = getUserDTO(record, now, currentUser.getLogin());
				upload.getUserDTOs().add(userDTO);

				if (userDTO.getMainContact()) {
					this.orgWithOwner.put(userDTO.getSalesforceId(), userDTO.getEmail());
				}
			}
		} catch (Exception e) {
			upload.addError(record.getRecordNumber(), getError("unexpected", e.getMessage(), currentUser));
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
		u.setMainContact(new Boolean(record.get("mainContact")));
		u.setLangKey("en");
		return u;
	}

	private boolean valid(CSVRecord record, UserUpload upload, User user) {
		String email = record.get("email");
		try {
			if (StringUtils.isBlank(email)) {
				upload.addError(record.getRecordNumber(), getError("missingEmail", user));
				return false;
			} else if (!emailValidator.isValid(email)) {
				upload.addError(record.getRecordNumber(), getError("invalidEmail", email, user));
				return false;
			} else {
				if (userExists(email)) {
					upload.addError(record.getRecordNumber(), getError("userExists", email, user));
					return false;
				}
			}
		} catch (IllegalArgumentException e) {
			upload.addError(record.getRecordNumber(), getError("missingEmail", user));
			return false;
		}

		String salesforceId = record.get("salesforceId");
		try {
			if (StringUtils.isBlank(salesforceId)) {
				upload.addError(record.getRecordNumber(), getError("missingSalesforceId", user));
				return false;
			} else if (!userService.memberExists(salesforceId)) {
				upload.addError(record.getRecordNumber(), getError("invalidSalesforceId", salesforceId, user));
				return false;
			}
		} catch (IllegalArgumentException e) {
			upload.addError(record.getRecordNumber(), getError("missingSalesforceId", user));
			return false;
		}

		Boolean isMain = new Boolean(record.get("mainContact"));
		if (isMain && orgWithOwner.containsKey(salesforceId)
				&& !StringUtils.equalsAnyIgnoreCase(orgWithOwner.get(salesforceId), email)) {
			upload.addError(record.getRecordNumber(), getError("multipleOrgOwners", user));
			return false;
		}

		return true;
	}

	private Boolean userExists(String email) {
		Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(email);
		return existingUser.isPresent();
	}

	private HashMap<String, String> getOrganizationsWithOwner() {
		List<User> users = userRepository.findAllByMainContactIsTrueAndDeletedIsFalse();
		HashMap<String, String> withOwners = new HashMap<String, String>();
		for (User user : users) {
			if (user.getMainContact()) {
				withOwners.put(user.getSalesforceId(), user.getEmail());
			}
		}
		return withOwners;
	}

	private String getError(String code, User user) {
		return getError(code, null, user);
	}

	private String getError(String code, String arg, User user) {
		return messageSource.getMessage("user.csv.upload.error." + code, arg != null ? new Object[] { arg } : null,
				Locale.forLanguageTag(user.getLangKey()));
	}
}
