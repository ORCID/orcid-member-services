package org.orcid.memberportal.service.member.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Properties specific to Member Service.
 * <p>
 * Properties are configured in the {@code application.yml} file. See
 * {@link io.github.jhipster.config.JHipsterProperties} for a good example.
 */
@Configuration
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private String orcidOrgClientId;

    private String orcidOrgSalesForceId;
    
    private String holisticsMemberDashboardUrl;
    
    private String holisticsMemberDashboardSecret;
    
    private String holisticsIntegrationDashboardUrl;
    
    private String holisticsIntegrationDashboardSecret;
    
    private String holisticsConsortiaDashboardUrl;
    
    private String holisticsConsortiaDashboardSecret;
    
    private String holisticsAffiliationDashboardUrl;
    
    private String holisticsAffiliationDashboardSecret;
    
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

    public String getHolisticsMemberDashboardUrl() {
        return holisticsMemberDashboardUrl;
    }

    public void setHolisticsMemberDashboardUrl(String holisticsMemberDashboardUrl) {
        this.holisticsMemberDashboardUrl = holisticsMemberDashboardUrl;
    }

    public String getHolisticsMemberDashboardSecret() {
        return holisticsMemberDashboardSecret;
    }

    public void setHolisticsMemberDashboardSecret(String holisticsMemberDashboardSecret) {
        this.holisticsMemberDashboardSecret = holisticsMemberDashboardSecret;
    }

    public String getHolisticsIntegrationDashboardUrl() {
        return holisticsIntegrationDashboardUrl;
    }

    public void setHolisticsIntegrationDashboardUrl(String holisticsIntegrationDashboardUrl) {
        this.holisticsIntegrationDashboardUrl = holisticsIntegrationDashboardUrl;
    }

    public String getHolisticsIntegrationDashboardSecret() {
        return holisticsIntegrationDashboardSecret;
    }

    public void setHolisticsIntegrationDashboardSecret(String holisticsIntegrationDashboardSecret) {
        this.holisticsIntegrationDashboardSecret = holisticsIntegrationDashboardSecret;
    }

    public String getHolisticsConsortiaDashboardUrl() {
        return holisticsConsortiaDashboardUrl;
    }

    public void setHolisticsConsortiaDashboardUrl(String holisticsConsortiaDashboardUrl) {
        this.holisticsConsortiaDashboardUrl = holisticsConsortiaDashboardUrl;
    }

    public String getHolisticsConsortiaDashboardSecret() {
        return holisticsConsortiaDashboardSecret;
    }

    public void setHolisticsConsortiaDashboardSecret(String holisticsConsortiaDashboardSecret) {
        this.holisticsConsortiaDashboardSecret = holisticsConsortiaDashboardSecret;
    }

    public String getHolisticsAffiliationDashboardUrl() {
        return holisticsAffiliationDashboardUrl;
    }

    public void setHolisticsAffiliationDashboardUrl(String holisticsAffiliationDashboardUrl) {
        this.holisticsAffiliationDashboardUrl = holisticsAffiliationDashboardUrl;
    }

    public String getHolisticsAffiliationDashboardSecret() {
        return holisticsAffiliationDashboardSecret;
    }

    public void setHolisticsAffiliationDashboardSecret(String holisticsAffiliationDashboardSecret) {
        this.holisticsAffiliationDashboardSecret = holisticsAffiliationDashboardSecret;
    }
    
}
