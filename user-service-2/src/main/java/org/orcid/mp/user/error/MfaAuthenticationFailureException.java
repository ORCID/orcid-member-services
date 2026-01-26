package org.orcid.mp.user.error;

import org.springframework.security.core.AuthenticationException;

public class MfaAuthenticationFailureException extends AuthenticationException {

    private static final long serialVersionUID = 1L;

    public MfaAuthenticationFailureException(String message) {
        super(message);
    }

    public MfaAuthenticationFailureException(String message, Throwable t) {
        super(message, t);
    }
}
