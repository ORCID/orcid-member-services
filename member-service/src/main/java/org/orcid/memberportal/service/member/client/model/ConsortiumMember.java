package org.orcid.memberportal.service.member.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsortiumMember {

    @JsonProperty("AccountId")
    private String salesforceId;
    
    @JsonProperty("Account")
    private Metadata metadata;

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }
    
    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public class Metadata {
        
        @JsonProperty("Public_Display_Name__c")
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
    
    
}
