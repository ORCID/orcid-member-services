package org.orcid.memberportal.service.user.domain;

import java.time.Instant;

public class ActivationReminder {
    
    private int daysElapsed;
    
    private Instant sentDate;
    
    public ActivationReminder(int daysElapsed, Instant sentDate) {
        this.daysElapsed = daysElapsed;
        this.sentDate = sentDate;
    }

    public int getDaysElapsed() {
        return daysElapsed;
    }

    public Instant getSentDate() {
        return sentDate;
    }
    
}
