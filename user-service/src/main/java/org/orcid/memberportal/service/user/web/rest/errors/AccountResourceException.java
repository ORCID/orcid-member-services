package org.orcid.memberportal.service.user.web.rest.errors;

public class AccountResourceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AccountResourceException(String message) {
        super(message);
    }

}
