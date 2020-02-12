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
}
