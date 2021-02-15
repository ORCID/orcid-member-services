package org.orcid.domain.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.enumeration.AssertionStatus;

public class AssertionUtils {
	
	public static String getAssertionStatus(Assertion assertion, OrcidRecord orcidRecord) {
		if(assertion.isUpdated()) {
			return AssertionStatus.PENDING_RETRY.getValue();
		}
		if (assertion.getOrcidError() != null) {
			JSONObject json = new JSONObject(assertion.getOrcidError());
			int statusCode = json.getInt("statusCode");
			String errorMessage = json.getString("error");
			switch (statusCode) {
			case 404:
				return AssertionStatus.USER_DELETED_FROM_ORCID.getValue();
			case 401:
				return AssertionStatus.USER_REVOKED_ACCESS.getValue();
			case 400:
				if(errorMessage.contains("invalid_scope")) {
					return AssertionStatus.USER_REVOKED_ACCESS.getValue();
				} else {
					if(!StringUtils.isBlank(assertion.getPutCode())) {
							return AssertionStatus.ERROR_UPDATING_TO_ORCID.getValue();
					}else {
						return AssertionStatus.ERROR_ADDING_TO_ORCID.getValue();
					}		
				}	
			default:
				if(!StringUtils.isBlank(assertion.getPutCode())) {
					return AssertionStatus.ERROR_UPDATING_TO_ORCID.getValue();
				}else {
					return AssertionStatus.ERROR_ADDING_TO_ORCID.getValue();
				}
			}

		}
		if (StringUtils.isBlank(assertion.getPutCode())) {
			if (orcidRecord.getDeniedDate() != null) {
				return AssertionStatus.USER_DENIED_ACCESS.getValue();
			}
			return AssertionStatus.PENDING.getValue();
		} else {
			if (assertion.getDeletedFromORCID() != null) {
				return AssertionStatus.DELETED_IN_ORCID.getValue();
			}
			return AssertionStatus.IN_ORCID.getValue();
		}
	}

}
