package org.orcid.memberportal.service.member.client.model;

public class MemberUpdateData {

    private String name;

    private String description;

    private String website;

    private String email;

    private String salesforceId;

    private String trademarkLicense;

    private BillingAddress billingAddress;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }

    public String getTrademarkLicense() {
        return trademarkLicense;
    }

    public void setTrademarkLicense(String trademarkLicense) {
        this.trademarkLicense = trademarkLicense;
    }

    public BillingAddress getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(BillingAddress billingAddress) {
        this.billingAddress = billingAddress;
    }
}
