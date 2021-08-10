package org.orcid.user.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

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
import org.springframework.stereotype.Component;

@Component
public class UserCsvReader implements UserUploadReader {

    private static final Logger LOG = LoggerFactory.getLogger(UserCsvReader.class);

    @Autowired
    private UserValidator userValidator;

    @Override
    public UserUpload readUsersUpload(InputStream inputStream, User currentUser) throws IOException {
        UserUpload upload = new UserUpload();
        Instant now = Instant.now();

        try (final Reader reader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
                final CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

            for (CSVRecord record : parser) {
                try {
                    UserDTO userDTO = getUserDTO(record, now, currentUser.getEmail());
                    UserValidation userValidation = userValidator.validate(userDTO, currentUser);
                    if (userValidation.isValid()) {
                        upload.getUserDTOs().add(userDTO);
                    } else {
                        for (String error : userValidation.getErrors()) {
                            upload.addError(record.getRecordNumber(), error);
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

    private UserDTO getUserDTO(CSVRecord record, Instant now, String createdBy) {
        UserDTO u = new UserDTO();
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
        u.setMainContact(false);
        u.setLangKey("en");
        return u;
    }

}
