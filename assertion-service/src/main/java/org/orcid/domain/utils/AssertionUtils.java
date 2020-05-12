package org.orcid.domain.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.enumeration.AssertionStatus;

public class AssertionUtils {
	
	public static String getAssertionStatus(Assertion assertion, OrcidRecord orcidRecord) {
		if (assertion.getOrcidError() != null) {
			JSONObject json = new JSONObject(assertion.getOrcidError());
			int statusCode = json.getInt("statusCode");
			switch (statusCode) {
			case 404:
				return AssertionStatus.USER_DELETED_FROM_ORCID.value;
			case 403:
				return AssertionStatus.USER_REVOKED_ACCESS.value;
			default:
				return AssertionStatus.ERROR_ADDIN_TO_ORCID.value;
			}

		}
		if (StringUtils.isBlank(assertion.getPutCode())) {
			if (orcidRecord.getDeniedDate() != null) {
				return AssertionStatus.USER_DENIED_ACCESS.value;
			}
			if (!StringUtils.isBlank(orcidRecord.getIdToken())) {
				return AssertionStatus.USER_GRANTED_ACCESS.value;
			}
			return AssertionStatus.PENDING.value;
		} else {
			if (assertion.getDeletedFromORCID() != null) {
				return AssertionStatus.DELETED_IN_ORCID.value;
			}
			return AssertionStatus.IN_ORCID.value;
		}
	}

}
