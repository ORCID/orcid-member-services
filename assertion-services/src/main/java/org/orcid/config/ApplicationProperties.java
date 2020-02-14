package org.orcid.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Assertion Services.
 * <p>
 * Properties are configured in the {@code application.yml} file. See
 * {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {
    private String jwtSignatureUrl;

    private String landingPageUrl;

    private String orcidAPIEndpoint;

    private TokenExchange tokenExchange;

    private Cron cron;

    public String getJwtSignatureUrl() {
        return jwtSignatureUrl;
    }

    public void setJwtSignatureUrl(String jwtSignatureUrl) {
        this.jwtSignatureUrl = jwtSignatureUrl;
    }

    public String getLandingPageUrl() {
        return landingPageUrl;
    }

    public void setLandingPageUrl(String landingPageUrl) {
        this.landingPageUrl = landingPageUrl;
    }

    public TokenExchange getTokenExchange() {
        return tokenExchange;
    }

    public void setTokenExchange(TokenExchange tokenExchange) {
        this.tokenExchange = tokenExchange;
    }

    public Cron getCron() {
        return cron;
    }

    public void setCron(Cron cron) {
        this.cron = cron;
    }

    public String getOrcidAPIEndpoint() {
        return orcidAPIEndpoint;
    }

    public void setOrcidAPIEndpoint(String orcidAPIEndpoint) {
        this.orcidAPIEndpoint = orcidAPIEndpoint;
    }

    public static class TokenExchange {
        private String endpoint;
        private String grantType;
        private String subjectTokenType;
        private String requestedTokenType;
        private String clientId;
        private String clientSecret;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getGrantType() {
            return grantType;
        }

        public void setGrantType(String grantType) {
            this.grantType = grantType;
        }

        public String getSubjectTokenType() {
            return subjectTokenType;
        }

        public void setSubjectTokenType(String subjectTokenType) {
            this.subjectTokenType = subjectTokenType;
        }

        public String getRequestedTokenType() {
            return requestedTokenType;
        }

        public void setRequestedTokenType(String requestedTokenType) {
            this.requestedTokenType = requestedTokenType;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

    }

    public static class Cron {
        private String postAffiliations;
        private String putAffiliations;

        public String getPostAffiliations() {
            return postAffiliations;
        }

        public void setPostAffiliations(String postAffiliations) {
            this.postAffiliations = postAffiliations;
        }

        public String getPutAffiliations() {
            return putAffiliations;
        }

        public void setPutAffiliations(String putAffiliations) {
            this.putAffiliations = putAffiliations;
        }
    }
}
