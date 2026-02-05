package org.orcid.mp.assertion.domain;

public enum AssertionStatus {

    USER_DENIED_ACCESS("User denied access"),
    PENDING("Pending"),
    IN_ORCID("In ORCID"),
    USER_GRANTED_ACCESS("User granted access"),
    USER_DELETED_FROM_ORCID("User deleted from ORCID"),
    USER_REVOKED_ACCESS("User revoked access"),
    ERROR_ADDING_TO_ORCID("Error adding to ORCID"),
    ERROR_UPDATING_TO_ORCID("Error updating in ORCID"),
    PENDING_RETRY("Pending retry in ORCID"),
    ERROR_DELETING_IN_ORCID("Error deleting in ORCID"),
    NOTIFICATION_REQUESTED("Notification requested"),
    NOTIFICATION_SENT("Notification sent"),
    NOTIFICATION_FAILED("Notification failed"),
    PENDING_UPDATE("Pending update in ORCID"),
    RECORD_DEACTIVATED_OR_DEPRECATED("Record is deactivated or deprecated");


    private final String value;

    AssertionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}