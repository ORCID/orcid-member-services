package org.orcid.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Properties specific to Member Service.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@Configuration
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {
	private String orcidOrgClientId;
	private String orcidOrgSalesForceId;

	public String getOrcidOrgClientId() {
		return orcidOrgClientId;
	}

	public void setOrcidOrgClientId(String orcidOrgClientId) {
		this.orcidOrgClientId = orcidOrgClientId;
	}

	public String getOrcidOrgSalesForceId() {
		return orcidOrgSalesForceId;
	}

	public void setOrcidOrgSalesForceId(String orcidOrgSalesForceId) {
		this.orcidOrgSalesForceId = orcidOrgSalesForceId;
	}

}
