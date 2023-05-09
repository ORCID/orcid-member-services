package org.orcid.memberportal.service.member.web.rest.vm;

import org.orcid.memberportal.service.member.client.model.MemberContact;

import java.util.List;

public class MemberContactUpdate {

    private String contactName;

    private String contactEmail;

    private String contactMember;

    private String contactNewFirstName;

    private String contactNewLastName;

    private String contactNewEmail;

    private String contactNewJobTitle;

    private String contactNewPhone;

    private List<String> contactNewRoles;

    private String requestedByName;

    private String requestedByEmail;

    private String requestedByMember;

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactMember() {
        return contactMember;
    }

    public void setContactMember(String contactMember) {
        this.contactMember = contactMember;
    }

    public String getContactNewFirstName() {
        return contactNewFirstName;
    }

    public void setContactNewFirstName(String contactNewFirstName) {
        this.contactNewFirstName = contactNewFirstName;
    }

    public String getContactNewLastName() {
        return contactNewLastName;
    }

    public void setContactNewLastName(String contactNewLastName) {
        this.contactNewLastName = contactNewLastName;
    }

    public String getContactNewEmail() {
        return contactNewEmail;
    }

    public void setContactNewEmail(String contactNewEmail) {
        this.contactNewEmail = contactNewEmail;
    }

    public String getContactNewJobTitle() {
        return contactNewJobTitle;
    }

    public void setContactNewJobTitle(String contactNewJobTitle) {
        this.contactNewJobTitle = contactNewJobTitle;
    }

    public String getContactNewPhone() {
        return contactNewPhone;
    }

    public void setContactNewPhone(String contactNewPhone) {
        this.contactNewPhone = contactNewPhone;
    }

    public List<String> getContactNewRoles() {
        return contactNewRoles;
    }

    public void setContactNewRoles(List<String> contactNewRoles) {
        this.contactNewRoles = contactNewRoles;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public String getRequestedByEmail() {
        return requestedByEmail;
    }

    public void setRequestedByEmail(String requestedByEmail) {
        this.requestedByEmail = requestedByEmail;
    }

    public String getRequestedByMember() {
        return requestedByMember;
    }

    public void setRequestedByMember(String requestedByMember) {
        this.requestedByMember = requestedByMember;
    }
}
