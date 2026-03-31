package org.orcid.mp.user.security;

import org.springframework.security.core.AuthenticationException;

public class MfaInvalidCodeException extends AuthenticationException {

    public MfaInvalidCodeException(String msg) {
        super(msg);
    }

    public MfaInvalidCodeException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
