package org.orcid.user.config;

/**
 * Application constants.
 */
public final class Constants {

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^([^@\\s]|(\".+\"))+@([^@\\s\\.\"'\\(\\)\\[\\]\\{\\}\\\\/,:;]+\\.)+([^@\\s\\.\"'\\(\\)\\[\\]\\{\\}\\\\/,:;]{2,})+$";
    public static final String SYSTEM_ACCOUNT = "system";
    public static final String DEFAULT_LANGUAGE = "en";
    public static final String ANONYMOUS_USER = "anonymoususer";

    private Constants() {
    }
}
