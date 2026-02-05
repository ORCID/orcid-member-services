package org.orcid.mp.assertion.pojo;

public class NotificationRequestInProgress {

    private final Boolean inProgress;

    public NotificationRequestInProgress(Boolean inProgress) {
        this.inProgress = inProgress;
    }

    public Boolean getInProgress() {
        return inProgress;
    }

}