package org.orcid.memberportal.service.member.web.rest.vm;

import java.util.List;

public class MemberContactUpdate {

    private String contactName;

    private String contactEmail;

    private String contactMember;

    private String contactNewName;

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

    public String getContactNewName() {
        return contactNewName;
    }

    public void setContactNewName(String contactNewName) {
        this.contactNewName = contactNewName;
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
