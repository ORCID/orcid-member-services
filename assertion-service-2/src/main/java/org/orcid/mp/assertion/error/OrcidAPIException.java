package org.orcid.mp.assertion.error;

public class OrcidAPIException extends RuntimeException {
    private static final long serialVersionUID = -3074497601567372837L;
    private final Integer statusCode;
    private final String error;

    public OrcidAPIException(Integer statusCode, String error) {
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