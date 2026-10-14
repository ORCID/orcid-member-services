package org.orcid.mp.member.apicreds;

import java.util.ArrayList;
import java.util.List;

/**
 * Data for the "Update to your ORCID Member API Credentials" email. Only the fields that
 * changed are set, so the email can list exactly what was updated.
 */
public class ApiClientUpdateNotification {

    private List<String> recipients = new ArrayList<>();

    private String clientId;

    private String clientName;

    private String homepageUrl;

    private String description;

    private List<String> addedRedirectUris = new ArrayList<>();

    private List<String> removedRedirectUris = new ArrayList<>();

    private boolean secretReset;

    public boolean hasChanges() {
        return homepageUrl != null || description != null || !addedRedirectUris.isEmpty()
                || !removedRedirectUris.isEmpty() || secretReset;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }

    public void setHomepageUrl(String homepageUrl) {
        this.homepageUrl = homepageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAddedRedirectUris() {
        return addedRedirectUris;
    }

    public void setAddedRedirectUris(List<String> addedRedirectUris) {
        this.addedRedirectUris = addedRedirectUris;
    }

    public List<String> getRemovedRedirectUris() {
        return removedRedirectUris;
    }

    public void setRemovedRedirectUris(List<String> removedRedirectUris) {
        this.removedRedirectUris = removedRedirectUris;
    }

    public boolean isSecretReset() {
        return secretReset;
    }

    public void setSecretReset(boolean secretReset) {
        this.secretReset = secretReset;
    }
}
