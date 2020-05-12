package org.orcid.user.web.rest;

import org.orcid.user.domain.Member;

import io.micrometer.core.instrument.util.StringUtils;

public class MemberValidator {

	public static boolean validate(Member memberSettings) {
		boolean isOk = true;
		if (StringUtils.isBlank(memberSettings.getClientId())) {
			isOk = false;
			memberSettings.setError("Client id should not be empty");
		}

		if (StringUtils.isBlank(memberSettings.getSalesforceId())) {
			isOk = false;
			memberSettings.setError("Salesforce id should not be empty");
		}
		
		if (StringUtils.isBlank(memberSettings.getParentSalesforceId()) && (memberSettings.getIsConsortiumLead() == null || !memberSettings.getIsConsortiumLead())) {
			isOk = false;
			memberSettings.setError("Parent salesforce id should not be empty if it is not a consortium lead");
		}
		return isOk;
	}

}
