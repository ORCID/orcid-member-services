package org.orcid.memberportal.service.gateway.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    public static final String ROLE_ORG_OWNER = "ROLE_ORG_OWNER";

    public static final String ASSERTION_SERVICE_ENABLED = "ASSERTION_SERVICE_ENABLED";

    private AuthoritiesConstants() {
    }
}
