package org.orcid.mp.user.error;

public class BadRequestAlertException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BadRequestAlertException(String message) {
        super(message);
    }
}
