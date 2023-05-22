package org.orcid.memberportal.service.member.web.rest.vm;

public class RemoveConsortiumMember {

    private String requestedByName;

    private String requestedByEmail;

    private String orgName;

    private String terminationMonth;

    private String terminationYear;

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

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getTerminationMonth() {
        return terminationMonth;
    }

    public void setTerminationMonth(String terminationMonth) {
        this.terminationMonth = terminationMonth;
    }

    public String getTerminationYear() {
        return terminationYear;
    }

    public void setTerminationYear(String terminationYear) {
        this.terminationYear = terminationYear;
    }
}
