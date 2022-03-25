package org.orcid.memberportal.service.assertion.web.rest.vm;

public class AssertionDeletion {
    
    private boolean deleted;
    
    public AssertionDeletion(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
    
}
