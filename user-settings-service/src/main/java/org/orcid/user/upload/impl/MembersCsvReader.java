package org.orcid.user.upload.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.orcid.user.domain.MemberSettings;
import org.orcid.user.domain.validation.MemberSettingsValidator;
import org.orcid.user.upload.MembersUpload;
import org.orcid.user.upload.MembersUploadReader;
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
				MemberSettings memberSettings = parseLine(line);
				
				if (!MemberSettingsValidator.validate(memberSettings)) {
					upload.addError(index, memberSettings.getError());
				} else {
					upload.addMemberSettings(memberSettings);
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

	private MemberSettings parseLine(CSVRecord record) {
		MemberSettings ms = new MemberSettings();
		if (record.isSet("assertionServiceEnabled")) {
			ms.setAssertionServiceEnabled(Boolean.parseBoolean(record.get("assertionServiceEnabled")));
		} else {
			ms.setAssertionServiceEnabled(false);
		}
		ms.setClientId(record.get("clientId"));
		Boolean isConsortiumLead = false;
		if (record.isSet("isConsortiumLead")) {
			isConsortiumLead = Boolean.parseBoolean(record.get("isConsortiumLead"));
		}
		ms.setIsConsortiumLead(isConsortiumLead);
		ms.setSalesforceId(record.get("salesforceId"));

		if (!isConsortiumLead) {
			ms.setParentSalesforceId(record.get("parentSalesforceId"));
		}
		if (record.isSet("clientName")) {
			ms.setClientName(record.get("clientName"));
		}
		return ms;
	}

}
