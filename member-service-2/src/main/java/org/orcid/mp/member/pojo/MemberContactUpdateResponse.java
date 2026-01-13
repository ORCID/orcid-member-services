package org.orcid.mp.member.pojo;

public class MemberContactUpdateResponse {

    private boolean successful;

    public MemberContactUpdateResponse(boolean successful) {
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
}