package org.orcid.user.web.rest.vm;

public class PasswordResetResultVM {

    private boolean success;

    private boolean expiredKey;

    private boolean invalidKey;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isExpiredKey() {
        return expiredKey;
    }

    public void setExpiredKey(boolean expiredKey) {
        this.expiredKey = expiredKey;
    }

    public boolean isInvalidKey() {
        return invalidKey;
    }

    public void setInvalidKey(boolean invalidKey) {
        this.invalidKey = invalidKey;
    }

}
