package org.orcid.domain.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.orcid.domain.Assertion;
import org.orcid.domain.OrcidRecord;
import org.orcid.domain.enumeration.AssertionStatus;

public class AssertionUtils {
    private static final String GRID_BASE_URL = "https://www.grid.ac/";
    private static final String GRID_BASE_URL_INSTITUTES = "https://www.grid.ac/institutes/";
    private static final String GRID_BASE_URL_ALT = "https://grid.ac/";
    private static final String GRID_BASE_URL_INSTITUTES_ALT = "https://grid.ac/institutes/";
	
	public static String getAssertionStatus(Assertion assertion, OrcidRecord orcidRecord) {
		if(assertion.isUpdated() && assertion.getAddedToORCID()!= null) {
			return AssertionStatus.PENDING_RETRY.getValue();
		}
		
		if(assertion.isUpdated() && assertion.getAddedToORCID()== null) {
			return AssertionStatus.PENDING.getValue();
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
			if (orcidRecord.getRevokedDate(assertion.getSalesforceId()) != null) {
				return AssertionStatus.USER_REVOKED_ACCESS.getValue();
			}
			if (orcidRecord.getDeniedDate(assertion.getSalesforceId()) != null) {
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
	
	public static String stripGridURL(String gridIdentifier) {
		if(! StringUtils.isBlank(gridIdentifier)) {	
			if(gridIdentifier.startsWith(GRID_BASE_URL_INSTITUTES)) {
				gridIdentifier = gridIdentifier.substring(GRID_BASE_URL_INSTITUTES.length());
			} 
			else if(gridIdentifier.startsWith(GRID_BASE_URL)) {
				gridIdentifier = gridIdentifier.substring(GRID_BASE_URL.length());
			}
			else if(gridIdentifier.startsWith(GRID_BASE_URL_INSTITUTES_ALT)) {
				gridIdentifier = gridIdentifier.substring(GRID_BASE_URL_INSTITUTES_ALT.length());
			}
			else if(gridIdentifier.startsWith(GRID_BASE_URL_ALT)) {
				gridIdentifier = gridIdentifier.substring(GRID_BASE_URL_ALT.length());
			}	
		}
		return gridIdentifier;
	}

}
