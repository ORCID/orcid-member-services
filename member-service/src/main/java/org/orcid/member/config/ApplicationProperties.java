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
	
	private String chartioOrgId;
	
	private String chartioSecret;
	
	private String chartioMemberDashboardId;
	
	private String chartioMemberDashboardUrl;
	
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

	public String getChartioOrgId() {
		return chartioOrgId;
	}

	public void setChartioOrgId(String chartioOrgId) {
		this.chartioOrgId = chartioOrgId;
	}

	public String getChartioSecret() {
		return chartioSecret;
	}

	public void setChartioSecret(String chartioSecret) {
		this.chartioSecret = chartioSecret;
	}

	public String getChartioMemberDashboardId() {
		return chartioMemberDashboardId;
	}

	public void setChartioMemberDashboardId(String chartioMemberDashboardId) {
		this.chartioMemberDashboardId = chartioMemberDashboardId;
	}

	public String getChartioMemberDashboardUrl() {
		return chartioMemberDashboardUrl;
	}

	public void setChartioMemberDashboardUrl(String chartioMemberDashboardUrl) {
		this.chartioMemberDashboardUrl = chartioMemberDashboardUrl;
	}
	
}
