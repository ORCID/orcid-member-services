package org.orcid.mp.member.apicreds;

public class AMCredentialsApplication {

    private String consortiumLeadName;

    private String requestedByName;

    private String requestedByEmail;

    private String orgName;

    private String notes;

    private String orgHomePage;

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public String getConsortiumLeadName() {
        return consortiumLeadName;
    }

    public void setConsortiumLeadName(String consortiumLeadName) {
        this.consortiumLeadName = consortiumLeadName;
    }

    public String getOrgHomePage() {
        return orgHomePage;
    }

    public void setOrgHomePage(String orgHomePage) {
        this.orgHomePage = orgHomePage;
    }
}