package org.orcid.mp.user.domain;

import org.springframework.data.mongodb.core.mapping.Field;

public class Member {

    private String clientId;

    private String salesforceId;

    private String memberId;

    private String parentSalesforceId;

    private String clientName;

    private Boolean assertionServiceEnabled;

    private Boolean superadminEnabled;

    private Boolean isConsortiumLead;

    private boolean active;

    public Boolean getIsConsortiumLead() {
        return isConsortiumLead;
    }

    public void setIsConsortiumLead(Boolean isConsortiumLead) {
        this.isConsortiumLead = isConsortiumLead;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getParentSalesforceId() {
        return parentSalesforceId;
    }

    public void setParentSalesforceId(String parentSalesforceId) {
        this.parentSalesforceId = parentSalesforceId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Boolean getAssertionServiceEnabled() {
        return assertionServiceEnabled;
    }

    public void setAssertionServiceEnabled(Boolean assertionServiceEnabled) {
        this.assertionServiceEnabled = assertionServiceEnabled;
    }

    public Boolean getSuperadminEnabled() {
        return superadminEnabled;
    }

    public void setSuperadminEnabled(Boolean superadminEnabled) {
        this.superadminEnabled = superadminEnabled;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}