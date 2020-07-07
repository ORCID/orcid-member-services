package org.orcid.member.web.rest;

import org.orcid.member.domain.Member;

import io.micrometer.core.instrument.util.StringUtils;

public class MemberValidator {

    public static boolean validate(Member member) {
		boolean isOk = true;

        validateField(member.getClientId(), "Client id should not be empty", member);

        validateField(member.getSalesforceId(), "Salesforce id should not be empty", member);

        validateField(member.getClientName(), "Member name should not be empty", member);

        try {
            if (StringUtils.isBlank(member.getParentSalesforceId()) && (member.getIsConsortiumLead() == null || !member.getIsConsortiumLead())) {
                member.setError("Parent salesforce id should not be empty if it is not a consortium lead");
            }
        } catch (IllegalArgumentException e) {
            member.setError("Parent salesforce id should not be empty if it is not a consortium lead");
        }

        if (member.getError() != null) {
            isOk = false;
        }

		return isOk;
	}

	private static void validateField(String value, String error, Member member) {
        try {
            if (StringUtils.isBlank(value)) {
                member.setError(error);
            }
        } catch (IllegalArgumentException e) {
            member.setError(error);
        }
    }

}
