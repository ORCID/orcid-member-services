package org.orcid.memberportal.service.member.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value="member")
public class MemberDetails {
    
    @JsonProperty("Id")
    private String id;
    
    @JsonProperty("Consortia_Member__c")
    private boolean consortiaMember;
    
    @JsonProperty("Consortium_Lead__c")
    private String consortiaLeadId;
    
    @JsonProperty("Name")
    private String name;
    
    @JsonProperty("Public_Display_Name__c")
    private String publicDisplayName;
    
    @JsonProperty("Website")
    private String website;
    
    @JsonProperty("BillingCountry")
    private String billingCountry;
    
    @JsonProperty("Research_Community__c")
    private String memberType;
    
    @JsonProperty("RecordTypeId")
    private String recordTypeId;
    
    @JsonProperty("Public_Display_Description__c")
    private String publicDisplayDescriptionHtml;
    
    @JsonProperty("Logo_Description__c")
    private String logoUrl;
    
    @JsonProperty("Public_Display_Email__c")
    private String publicDisplayEmail;
    
    @JsonProperty("Last_membership_start_date__c")
    private String membershipStartDateString;
    
    @JsonProperty("Last_membership_end_date__c")
    private String membershipEndDateString;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isConsortiaMember() {
        return consortiaMember;
    }

    public void setConsortiaMember(boolean consortiaMember) {
        this.consortiaMember = consortiaMember;
    }

    public String getConsortiaLeadId() {
        return consortiaLeadId;
    }

    public void setConsortiaLeadId(String consortiaLeadId) {
        this.consortiaLeadId = consortiaLeadId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublicDisplayName() {
        return publicDisplayName;
    }

    public void setPublicDisplayName(String publicDisplayName) {
        this.publicDisplayName = publicDisplayName;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getBillingCountry() {
        return billingCountry;
    }

    public void setBillingCountry(String billingCountry) {
        this.billingCountry = billingCountry;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public String getRecordTypeId() {
        return recordTypeId;
    }

    public void setRecordTypeId(String recordTypeId) {
        this.recordTypeId = recordTypeId;
    }

    public String getPublicDisplayDescriptionHtml() {
        return publicDisplayDescriptionHtml;
    }

    public void setPublicDisplayDescriptionHtml(String publicDisplayDescriptionHtml) {
        this.publicDisplayDescriptionHtml = publicDisplayDescriptionHtml;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPublicDisplayEmail() {
        return publicDisplayEmail;
    }

    public void setPublicDisplayEmail(String publicDisplayEmail) {
        this.publicDisplayEmail = publicDisplayEmail;
    }

    public String getMembershipStartDateString() {
        return membershipStartDateString;
    }

    public void setMembershipStartDateString(String membershipStartDateString) {
        this.membershipStartDateString = membershipStartDateString;
    }

    public String getMembershipEndDateString() {
        return membershipEndDateString;
    }

    public void setMembershipEndDateString(String membershipEndDateString) {
        this.membershipEndDateString = membershipEndDateString;
    }
    
}
