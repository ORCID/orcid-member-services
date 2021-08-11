package org.orcid.domain.enumeration;

import java.util.Map;
import java.util.HashMap;

public enum AssertionStatus {

    USER_DENIED_ACCESS("USER_DENIED_ACCES", "User denied access"), PENDING("PENDING", "Pending"), DELETED_IN_ORCID("DELETED_IN_ORCID", "Deleted in ORCID"), IN_ORCID(
            "IN_ORCID", "In ORCID"), USER_GRANTED_ACCESS("USER_GRANTED_ACCESS", "User granted access"), USER_DELETED_FROM_ORCID("USER_DELETED_FROM_ORCID",
                    "User deleted from ORCID"), USER_REVOKED_ACCESS("USER_REVOKED_ACCESS", "User revoked access"), ERROR_ADDING_TO_ORCID("ERROR_ADDING_TO_ORCID",
                            "Error adding to ORCID"), ERROR_UPDATING_TO_ORCID("ERROR_UPDATING_TO_ORCID",
                                    "Error updating in ORCID"), PENDING_RETRY("PENDING_RETRY", "Pending retry in ORCID"), UNKNOWN("UNKNOWN", "Unknown");

    private final String value;
    private final String text;

    /**
     * A mapping between the string code as stored in DB and its corresponding
     * text to facilitate lookup by code.
     */
    private static Map<String, AssertionStatus> valueToTextMapping;

    private AssertionStatus(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public static AssertionStatus getStatus(String value) {
        if (valueToTextMapping == null) {
            initMapping();
        }
        return valueToTextMapping.get(value);
    }

    private static void initMapping() {
        valueToTextMapping = new HashMap<>();
        for (AssertionStatus s : values()) {
            valueToTextMapping.put(s.value, s);
        }
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return valueToTextMapping.get(value).text;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("AssertionStatus");
        sb.append("{value=").append(value);
        sb.append(", text='").append(text).append('\'');
        sb.append('}');
        return sb.toString();
    }

}
