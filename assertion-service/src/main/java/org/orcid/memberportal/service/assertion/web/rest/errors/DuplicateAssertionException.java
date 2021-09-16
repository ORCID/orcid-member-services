package org.orcid.web.rest.errors;

public class DuplicateAssertionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DuplicateAssertionException(String message) {
        super(message);
    }

}
