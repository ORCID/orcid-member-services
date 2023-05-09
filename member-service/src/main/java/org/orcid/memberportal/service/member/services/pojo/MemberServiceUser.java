package org.orcid.memberportal.service.member.services.pojo;

import java.time.Instant;
import java.util.Set;

public class MemberServiceUser {

    private String id;

    private String salesforceId;

    private String createdBy;

    private Instant createdDate;

    private String lastModifiedBy;

    private Instant lastModifiedDate;

    private String salesforceIdError;

    private String parentSalesforceId;

    private Boolean mainContact;

    private Boolean assertionServiceEnabled;

    private String firstName;

    private String lastName;

    private String email;

    private String imageUrl;

    private boolean activated;

    private String langKey;

    private Set<String> authorities;

    private String memberName;

    public String getLoginAs() {
        return loginAs;
    }

    public void setLoginAs(String loginAs) {
        this.loginAs = loginAs;
    }

    private String loginAs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getSalesforceIdError() {
        return salesforceIdError;
    }

    public void setSalesforceIdError(String salesforceIdError) {
        this.salesforceIdError = salesforceIdError;
    }

    public String getParentSalesforceId() {
        return parentSalesforceId;
    }

    public void setParentSalesforceId(String parentSalesforceId) {
        this.parentSalesforceId = parentSalesforceId;
    }

    public Boolean getMainContact() {
        return mainContact;
    }

    public void setMainContact(Boolean mainContact) {
        this.mainContact = mainContact;
    }

    public Boolean getAssertionServiceEnabled() {
        return assertionServiceEnabled;
    }

    public void setAssertionServiceEnabled(Boolean assertionServiceEnabled) {
        this.assertionServiceEnabled = assertionServiceEnabled;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Set<String> authorities) {
        this.authorities = authorities;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

}
