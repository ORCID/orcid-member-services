package org.orcid.mp.member.apicreds;

import java.util.List;

public class ProductionCredentialsApplication {

    private String consortiumLeadEmail;

    private List<String> redirectUris;

    private String requestedByName;

    private String requestedByEmail;

    private String orgName;

    private String systemIntegrationType;

    private String authenticateIdsAnswer;

    private String integrationDisplayName;

    private String integrationHomepageUrl;

    private String integrationDescription;

    private String notes;

    public List<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(List<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getSystemIntegrationType() {
        return systemIntegrationType;
    }

    public void setSystemIntegrationType(String systemIntegrationType) {
        this.systemIntegrationType = systemIntegrationType;
    }

    public String getAuthenticateIdsAnswer() {
        return authenticateIdsAnswer;
    }

    public void setAuthenticateIdsAnswer(String authenticateIdsAnswer) {
        this.authenticateIdsAnswer = authenticateIdsAnswer;
    }

    public String getIntegrationDisplayName() {
        return integrationDisplayName;
    }

    public void setIntegrationDisplayName(String integrationDisplayName) {
        this.integrationDisplayName = integrationDisplayName;
    }

    public String getIntegrationHomepageUrl() {
        return integrationHomepageUrl;
    }

    public void setIntegrationHomepageUrl(String integrationHomepageUrl) {
        this.integrationHomepageUrl = integrationHomepageUrl;
    }

    public String getIntegrationDescription() {
        return integrationDescription;
    }

    public void setIntegrationDescription(String integrationDescription) {
        this.integrationDescription = integrationDescription;
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

    public String getConsortiumLeadEmail() {
        return consortiumLeadEmail;
    }

    public void setConsortiumLeadEmail(String consortiumLeadEmail) {
        this.consortiumLeadEmail = consortiumLeadEmail;
    }
}
