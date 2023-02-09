package org.orcid.memberportal.service.assertion.domain;

import org.springframework.data.mongodb.core.mapping.Field;

public class AssertionServiceMember {

    @Field("client_id")
    private String clientId;

    @Field("salesforce_id")
    private String salesforceId;

    @Field("parent_salesforce_id")
    private String parentSalesforceId;

    @Field("client_name")
    private String clientName;

    @Field("assertion_service_enabled")
    private Boolean assertionServiceEnabled;

    @Field("superadmin_enabled")
    private Boolean superadminEnabled;

    @Field("is_consortium_lead")
    private Boolean isConsortiumLead;
    
    @Field("default_language")
    private String defaultLanguage;

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

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    
}
