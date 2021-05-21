package org.orcid.member.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.orcid.member.domain.Member;
import org.orcid.member.service.user.MemberServiceUser;
import org.orcid.member.upload.MemberUpload;
import org.orcid.member.upload.MembersUploadReader;
import org.orcid.member.validation.MemberValidation;
import org.orcid.member.validation.MemberValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class MemberCsvReader implements MembersUploadReader {

	private static final Logger LOG = LoggerFactory.getLogger(MemberCsvReader.class);

	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private MemberValidator memberValidator;

	@Override
	public MemberUpload readMemberUpload(InputStream inputStream, MemberServiceUser user) throws IOException {
		MemberUpload upload = new MemberUpload();

		try (final Reader reader = new InputStreamReader(new BOMInputStream(inputStream), StandardCharsets.UTF_8);
				final CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader())) {

			for (CSVRecord line : parser) {
				try {
					Member member = createMemberInstance(user);
					member = parseLine(line, member);

					MemberValidation validation = memberValidator.validate(member, user); 
					if (!validation.isValid()) {
						for (String error : validation.getErrors()) {
							upload.addError(line.getRecordNumber(), error);
						}
					} else {
						upload.addMember(member);
					}
				} catch (Exception e) {
					LOG.info("CSV upload error found for record number {}", line.getRecordNumber());
					upload.addError(line.getRecordNumber(), getError("unexpected", e.getMessage(), user));
				}
			}
		}
		return upload;
	}
	
	private Member createMemberInstance(MemberServiceUser user) {
		Instant now = Instant.now();
		Member member = new Member();
		member.setCreatedDate(now);
		member.setLastModifiedDate(now);
		member.setCreatedBy(user.getLogin());
		member.setLastModifiedBy(user.getLogin());
		return member;
	}

	private Member parseLine(CSVRecord record, Member member) {
		if (record.isSet("assertionServiceEnabled")) {
			member.setAssertionServiceEnabled(Boolean.parseBoolean(record.get("assertionServiceEnabled")));
		} else {
			member.setAssertionServiceEnabled(false);
		}

		if (record.isSet("clientId")) {
			member.setClientId(record.get("clientId"));
		}

		if (record.isSet("isConsortiumLead")) {
			member.setIsConsortiumLead(Boolean.parseBoolean(record.get("isConsortiumLead")));
		}

		if (record.isSet("salesforceId")) {
			member.setSalesforceId(record.get("salesforceId"));
		}
		
		if (record.isSet("parentSalesforceId")) {
			member.setParentSalesforceId(record.get("parentSalesforceId"));
		}
		
		if (record.isSet("clientName")) {
			member.setClientName(record.get("clientName"));
		}
		return member;
	}

	private String getError(String code, String arg, MemberServiceUser user) {
		return messageSource.getMessage("member.validation.error." + code, arg != null ? new Object[] { arg } : null,
				Locale.forLanguageTag(user.getLangKey()));
	}

}
