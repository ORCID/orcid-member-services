package org.orcid.member.web.rest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orcid.member.domain.Member;

import io.micrometer.core.instrument.util.StringUtils;

public class MemberValidator {
	private static final String OLD_CLIENT_ID_PATTERN = "[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$";
	private static final String NEW_CLIENT_ID_PREFIX = "APP-";
	private static final String NEW_CLIENT_ID_PATTERN = NEW_CLIENT_ID_PREFIX + "[A-Z0-9]{16}$";
	private static final Pattern oldPattern = Pattern.compile(OLD_CLIENT_ID_PATTERN);
	private static final Pattern newPattern = Pattern.compile(NEW_CLIENT_ID_PATTERN);

	public static boolean validate(Member member) {
		boolean isOk = true;
		if (!member.getIsConsortiumLead()) {
			validateField(member.getClientId(), "Client id should not be empty", member);
		}

		if (!StringUtils.isBlank(member.getClientId())) {
			if (member.getClientId().startsWith(NEW_CLIENT_ID_PREFIX)) {
				Matcher newMatcher = newPattern.matcher(member.getClientId());
				isOk = newMatcher.matches();
			} else {
				Matcher oldMatcher = oldPattern.matcher(member.getClientId());
				isOk = oldMatcher.matches();
			}
		}

		if (!isOk) {
			member.setError("Client id should be in the format XXXX-XXXX-XXXX-XXXX or"
					+ " APP-XXXXXXXXXXXXXXXX. X can be a digit or an uppercase character.");
		}

		if (member.getIsConsortiumLead() && StringUtils.isBlank(member.getClientId())) {
			member.setAssertionServiceEnabled(false);
		}

		validateField(member.getSalesforceId(), "Salesforce id should not be empty", member);

		validateField(member.getClientName(), "Member name should not be empty", member);
		try {
			if (StringUtils.isBlank(member.getParentSalesforceId())
					&& (member.getIsConsortiumLead() == null || !member.getIsConsortiumLead())) {
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
