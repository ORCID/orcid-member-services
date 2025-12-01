package org.orcid.mp.user.rest.error;

public class BadRequestAlertException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BadRequestAlertException(String message) {
        super(message);
    }
}
