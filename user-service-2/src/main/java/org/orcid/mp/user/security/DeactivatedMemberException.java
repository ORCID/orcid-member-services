package org.orcid.mp.user.security;

import org.springframework.security.core.AuthenticationException;

public class DeactivatedMemberException extends AuthenticationException {

    public DeactivatedMemberException() {
        super("Member is deactivated.");
    }
}
