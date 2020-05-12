package org.orcid.user.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.orcid.user.domain.Member;
import org.orcid.user.upload.MembersUpload;
import org.orcid.user.upload.MembersUploadReader;
import org.orcid.user.web.rest.MemberValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MembersCsvReader implements MembersUploadReader {

	private static final Logger LOG = LoggerFactory.getLogger(MembersCsvReader.class);

	@Override
	public MembersUpload readMembersUpload(InputStream inputStream) {
		InputStreamReader isr = new InputStreamReader(inputStream);
		Iterable<CSVRecord> elements = null;
		MembersUpload upload = new MembersUpload();

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
				Member member = parseLine(line);
				
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

	private Member parseLine(CSVRecord record) {
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
		return member;
	}

}
