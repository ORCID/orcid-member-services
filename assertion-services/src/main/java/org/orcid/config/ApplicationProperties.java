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

    public String getJwtSignatureUrl() {
        return jwtSignatureUrl;
    }

    public void setJwtSignatureUrl(String jwtSignatureUrl) {
        this.jwtSignatureUrl = jwtSignatureUrl;
    }
}
