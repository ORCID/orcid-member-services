package org.orcid.mp.member.error;

import java.util.List;

public class ApiClientValidationException extends RuntimeException {

    private final List<String> errors;

    public ApiClientValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
