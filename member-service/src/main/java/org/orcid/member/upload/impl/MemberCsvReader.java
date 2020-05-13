package org.orcid.member.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.orcid.member.domain.Member;
import org.orcid.member.upload.MemberUpload;
import org.orcid.member.upload.MembersUploadReader;
import org.orcid.member.web.rest.MemberValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MemberCsvReader implements MembersUploadReader {

	private static final Logger LOG = LoggerFactory.getLogger(MemberCsvReader.class);

	@Override
	public MemberUpload readMemberUpload(InputStream inputStream) {
		Instant now = Instant.now();
		InputStreamReader isr = new InputStreamReader(inputStream);
		Iterable<CSVRecord> elements = null;
		MemberUpload upload = new MemberUpload();

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
			for (CSVRecord line : elements) {
				long index = line.getRecordNumber();
				Member member = parseLine(line, now);
				
				if (!MemberValidator.validate(member)) {
					upload.addError(index, member.getError());
				} else {
					upload.addMemberSettings(member);
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

	private Member parseLine(CSVRecord record, Instant now) {
		Member member = new Member();
		if (record.isSet("assertionServiceEnabled")) {
			member.setAssertionServiceEnabled(Boolean.parseBoolean(record.get("assertionServiceEnabled")));
		} else {
			member.setAssertionServiceEnabled(false);
		}
		member.setClientId(record.get("clientId"));
		Boolean isConsortiumLead = false;
		if (record.isSet("isConsortiumLead")) {
			isConsortiumLead = Boolean.parseBoolean(record.get("isConsortiumLead"));
		}
		member.setIsConsortiumLead(isConsortiumLead);
		member.setSalesforceId(record.get("salesforceId"));

		if (!isConsortiumLead) {
			member.setParentSalesforceId(record.get("parentSalesforceId"));
		}
		if (record.isSet("clientName")) {
			member.setClientName(record.get("clientName"));
		}
		member.setCreatedDate(now);
		member.setLastModifiedDate(now);
		return member;
	}

}
