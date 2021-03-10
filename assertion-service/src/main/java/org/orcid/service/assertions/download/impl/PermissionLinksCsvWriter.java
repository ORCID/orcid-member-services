package org.orcid.service.assertions.download.impl;

import java.io.IOException;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.orcid.config.ApplicationProperties;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.repository.AssertionsRepository;
import org.orcid.security.EncryptUtil;
import org.orcid.service.OrcidRecordService;
import org.orcid.service.UserService;
import org.orcid.service.assertions.download.CsvWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PermissionLinksCsvWriter extends CsvWriter {
	
	@Autowired
	private EncryptUtil encryptUtil;

	@Autowired
	private ApplicationProperties applicationProperties;
	
	@Autowired
	private OrcidRecordService orcidRecordService;

	@Autowired
	private UserService assertionsUserService;

	@Autowired
	private AssertionsRepository assertionsRepository;

	@Override
	public String writeCsv() throws IOException {
		String landingPageUrl = applicationProperties.getLandingPageUrl();
		StringBuffer buffer = new StringBuffer();
		CSVPrinter csvPrinter = getCSVPrinterWithHeaders(buffer);
		
		String salesForceId = assertionsUserService.getLoggedInUserSalesforceId();
		List<OrcidRecord> records = orcidRecordService.getRecordsWithoutTokens(salesForceId);

		for (OrcidRecord record : records) {
			String email = record.getEmail();
			List<Assertion> assertions = assertionsRepository.findByEmailAndSalesforceId(email, salesForceId);
			if (assertions.size() > 0) {
				String encrypted = encryptUtil.encrypt(salesForceId + "&&" + email);
				String link = landingPageUrl + "?state=" + encrypted;
				csvPrinter.printRecord(email, link);
			}
		}

		csvPrinter.flush();
		csvPrinter.close();
		return buffer.toString();
	}

	private CSVPrinter getCSVPrinterWithHeaders(StringBuffer buffer) throws IOException {
		return new CSVPrinter(buffer, CSVFormat.DEFAULT.withHeader("email", "link"));
	}

}
