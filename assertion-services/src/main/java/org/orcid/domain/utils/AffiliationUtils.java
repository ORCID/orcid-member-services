package org.orcid.domain.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.enumeration.AffiliationStatus;

public class AffiliationUtils {
	
	public static String getAffiliationStatus(Assertion affiliation, OrcidRecord orcidRecord) {
		if (affiliation.getOrcidError() != null) {
			JSONObject json = new JSONObject(affiliation.getOrcidError());
			int statusCode = json.getInt("statusCode");
			switch (statusCode) {
			case 404:
				return AffiliationStatus.USER_DELETED_FROM_ORCID.value;
			case 403:
				return AffiliationStatus.USER_REVOKED_ACCESS.value;
			default:
				return AffiliationStatus.ERROR_ADDIN_TO_ORCID.value;
			}

		}
		if (StringUtils.isBlank(affiliation.getPutCode())) {
			if (orcidRecord.getDeniedDate() != null) {
				return AffiliationStatus.USER_DENIED_ACCESS.value;
			}
			if (!StringUtils.isBlank(orcidRecord.getIdToken())) {
				return AffiliationStatus.USER_GRANTED_ACCESS.value;
			}
			return AffiliationStatus.PENDING.value;
		} else {
			if (affiliation.getDeletedFromORCID() != null) {
				return AffiliationStatus.DELETED_IN_ORCID.value;
			}
			return AffiliationStatus.IN_ORCID.value;
		}
	}

}
