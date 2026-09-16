package org.orcid.mp.member.apicreds;

import java.time.LocalDateTime;

public class ApiClientSummary {
    private String clientDetailsId;
    private String memberId;
    private String clientName;
    private String clientType;
    private LocalDateTime dateCreated;
    private LocalDateTime lastModified;
    private boolean allowAutoDeprecate;
    private LocalDateTime deactivatedDate;

    public String getClientDetailsId() { return clientDetailsId; }
    public void setClientDetailsId(String clientDetailsId) { this.clientDetailsId = clientDetailsId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientType() { return clientType; }
    public void setClientType(String clientType) { this.clientType = clientType; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public boolean isAllowAutoDeprecate() { return allowAutoDeprecate; }
    public void setAllowAutoDeprecate(boolean allowAutoDeprecate) { this.allowAutoDeprecate = allowAutoDeprecate; }

    public LocalDateTime getDeactivatedDate() { return deactivatedDate; }
    public void setDeactivatedDate(LocalDateTime deactivatedDate) { this.deactivatedDate = deactivatedDate; }
}