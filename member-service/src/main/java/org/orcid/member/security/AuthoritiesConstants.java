package org.orcid.member.security;

/**
 * Constants for Spring Security authorities.
 */
public final class AuthoritiesConstants {

    public static final String ADMIN = "ROLE_ADMIN";

    public static final String USER = "ROLE_USER";

    public static final String ORG_OWNER = "ROLE_ORG_OWNER";

    public static final String CONSORTIUM_LEAD = "ROLE_CONSORTIUM_LEAD";

    public static final String ANONYMOUS = "ROLE_ANONYMOUS";

    private AuthoritiesConstants() {

    }
}
