package org.orcid.user.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.RandomStringUtils;
import org.orcid.user.domain.User;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.upload.UserUpload;
import org.orcid.user.upload.UserUploadReader;
import org.orcid.user.validation.UserValidation;
import org.orcid.user.validation.UserValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class UserCsvReader implements UserUploadReader {

    private static final Logger LOG = LoggerFactory.getLogger(UserCsvReader.class);

    @Autowired
    private UserValidator userValidator;

    @Autowired
    private MessageSource messageSource;

    @Override
    public UserUpload readUsersUpload(InputStream inputStream, User currentUser) throws IOException {
        UserUpload upload = new UserUpload();
        Instant now = Instant.now();
        Map<String, UserDTO> mainContacts = new HashMap<>();

        try (final Reader reader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
                final CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

            for (CSVRecord record : parser) {
                try {
                    UserDTO userDTO = getUserDTO(record, now, currentUser.getLogin());
                    UserValidation userValidation = userValidator.validate(userDTO, currentUser);
                    boolean mainContactViolation = duplicateMainContact(userDTO, mainContacts);
                    if (userValidation.isValid() && !mainContactViolation) {
                        upload.getUserDTOs().add(userDTO);
                    } else {
                        for (String error : userValidation.getErrors()) {
                            upload.addError(record.getRecordNumber(), error);
                        }

                        if (mainContactViolation) {
                            upload.addError(record.getRecordNumber(), getError("multipleOrgOwners", null, currentUser));
                        }
                    }
                } catch (Exception e) {
                    LOG.info("CSV upload error found for record number {}", record.getRecordNumber());
                    upload.addError(record.getRecordNumber(), e.getMessage());
                }
            }
        }
        return upload;
    }

    private boolean duplicateMainContact(UserDTO userDTO, Map<String, UserDTO> mainContacts) {
        UserDTO existingMainContact = mainContacts.get(userDTO.getSalesforceId());
        if (existingMainContact != null && !existingMainContact.getEmail().equals(userDTO.getEmail())) {
            return true;
        }

        mainContacts.put(userDTO.getSalesforceId(), userDTO);
        return false;
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

    private String getError(String code, String arg, User user) {
        return messageSource.getMessage("user.validation.error." + code, arg != null ? new Object[] { arg } : null,
                Locale.forLanguageTag(user.getLangKey()));
    }

}
