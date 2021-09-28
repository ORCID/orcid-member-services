package org.orcid.memberportal.service.assertion.web.rest.errors;

public class ORCIDAPIException extends RuntimeException {
    private static final long serialVersionUID = -3074497601567372837L;
    private final Integer statusCode;
    private final String error;

    public ORCIDAPIException(Integer statusCode, String error) {
        this.statusCode = statusCode;
        this.error = error;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }

}
