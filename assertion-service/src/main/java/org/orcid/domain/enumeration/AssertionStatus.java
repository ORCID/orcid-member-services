package org.orcid.domain.enumeration;

public enum AssertionStatus {
	
	USER_DENIED_ACCESS("User denied access"),
	PENDING("Pending"),
	DELETED_IN_ORCID("Deleted in ORCID"),
	IN_ORCID("In ORCID"),
	USER_GRANTED_ACCESS("User granted access"),
	USER_DELETED_FROM_ORCID("User deleted from ORCID"),
	USER_REVOKED_ACCESS("User revoked access"),
	ERROR_ADDING_TO_ORCID("Error adding to ORCID"),
	ERROR_UPDATING_TO_ORCID("Error updating in ORCID"),
	PENDING_RETRY("Pending retry in ORCID");
	
	public final String value;
	
	AssertionStatus(String value) {
		this.value = value;
	}
	
	

}
