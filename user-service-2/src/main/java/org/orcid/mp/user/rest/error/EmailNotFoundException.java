package org.orcid.mp.user.rest.error;

public class EmailNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public EmailNotFoundException() {
        super("Email address not registered");
    }
}
