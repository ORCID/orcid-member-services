package org.orcid.memberportal.service.assertion.web.rest.vm;

public class NotificationRequestInProgress {
    
    private Boolean inProgress;
    
    public NotificationRequestInProgress(Boolean inProgress) {
        this.inProgress = inProgress;
    }

    public Boolean getInProgress() {
        return inProgress;
    }

}
