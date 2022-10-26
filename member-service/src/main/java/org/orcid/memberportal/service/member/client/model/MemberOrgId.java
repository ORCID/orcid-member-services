package org.orcid.memberportal.service.member.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MemberOrgId {
    
    @JsonProperty("Identifier_Type__c")
    private String type;
    
    @JsonProperty("Name")
    private String value;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
}
