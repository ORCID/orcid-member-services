package org.orcid.mp.user.security;

import org.springframework.security.core.AuthenticationException;

public class MfaRequiredException extends AuthenticationException {

    public MfaRequiredException(String msg) {
        super(msg);
    }

    public MfaRequiredException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
