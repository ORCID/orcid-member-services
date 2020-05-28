package org.orcid.member.web.rest;

import org.orcid.member.domain.Member;

import io.micrometer.core.instrument.util.StringUtils;

public class MemberValidator {

	public static boolean validate(Member member) {
		boolean isOk = true;
		if (StringUtils.isBlank(member.getClientId())) {
			isOk = false;
			member.setError("Client id should not be empty");
		}

		if (StringUtils.isBlank(member.getSalesforceId())) {
			isOk = false;
			member.setError("Salesforce id should not be empty");
		}
		
		if (StringUtils.isBlank(member.getClientName())) {
                    isOk = false;
                    member.setError("Member name should not be empty");
                }
		
		if (StringUtils.isBlank(member.getParentSalesforceId()) && (member.getIsConsortiumLead() == null || !member.getIsConsortiumLead())) {
			isOk = false;
			member.setError("Parent salesforce id should not be empty if it is not a consortium lead");
		}
		return isOk;
	}

}
