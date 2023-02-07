package org.orcid.memberportal.service.assertion.config;

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

    private String mailDomain;

    private String mailApiKey;

    private String mailApiUrl;

    private String mailFromAddress;

    private String mailFromName;

    private boolean mailTestMode;

    private String memberAssertionStatsRecipient;

    private int storedFileLifespan;

    private String memberAssertionStatsDirectory;
    
    private String syncAffiliationsDelay;

    private String generateMemberAssertionStatsCron;
    
    private String assertionsCsvUploadDirectory;
    
    private String processAssertionUploadsDelay;
    
    private String csvReportsDirectory;
    
    private String removeStoredFilesDelay;
    
    private String processCsvReportsDelay;
    
    private String internalRegistryApiEndpoint;
    
    private String sendPermissionLinkNotificationsDelay;
    
    private int[] resendNotificationDays;
    
    private String resendNotificationsCron;
    
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

    public String getOrcidAPIEndpoint() {
        return orcidAPIEndpoint;
    }

    public void setOrcidAPIEndpoint(String orcidAPIEndpoint) {
        this.orcidAPIEndpoint = orcidAPIEndpoint;
    }

    public boolean isMailTestMode() {
        return mailTestMode;
    }

    public void setMailTestMode(boolean mailTestMode) {
        this.mailTestMode = mailTestMode;
    }

    public String getMailDomain() {
        return mailDomain;
    }

    public void setMailDomain(String mailDomain) {
        this.mailDomain = mailDomain;
    }

    public String getMailApiKey() {
        return mailApiKey;
    }

    public void setMailApiKey(String mailApiKey) {
        this.mailApiKey = mailApiKey;
    }

    public String getMailApiUrl() {
        return mailApiUrl;
    }

    public void setMailApiUrl(String mailApiUrl) {
        this.mailApiUrl = mailApiUrl;
    }

    public String getMailFromAddress() {
        return mailFromAddress;
    }

    public void setMailFromAddress(String mailFromAddress) {
        this.mailFromAddress = mailFromAddress;
    }

    public String getMailFromName() {
        return mailFromName;
    }

    public void setMailFromName(String mailFromName) {
        this.mailFromName = mailFromName;
    }

    public String getMemberAssertionStatsRecipient() {
        return memberAssertionStatsRecipient;
    }

    public void setMemberAssertionStatsRecipient(String memberAssertionStatsRecipient) {
        this.memberAssertionStatsRecipient = memberAssertionStatsRecipient;
    }

    public int getStoredFileLifespan() {
        return storedFileLifespan;
    }

    public void setStoredFileLifespan(int storedFileLifespan) {
        this.storedFileLifespan = storedFileLifespan;
    }

    public String getMemberAssertionStatsDirectory() {
        return memberAssertionStatsDirectory;
    }

    public void setMemberAssertionStatsDirectory(String memberAssertionStatsDirectory) {
        this.memberAssertionStatsDirectory = memberAssertionStatsDirectory;
    }
    
    public String getSyncAffiliationsDelay() {
        return syncAffiliationsDelay;
    }

    public void setSyncAffiliationsDelay(String syncAffiliationsDelay) {
        this.syncAffiliationsDelay = syncAffiliationsDelay;
    }

    public String getGenerateMemberAssertionStatsCron() {
        return generateMemberAssertionStatsCron;
    }

    public void setGenerateMemberAssertionStatsCron(String generateMemberAssertionStatsCron) {
        this.generateMemberAssertionStatsCron = generateMemberAssertionStatsCron;
    }

    public String getAssertionsCsvUploadDirectory() {
        return assertionsCsvUploadDirectory;
    }

    public void setAssertionsCsvUploadDirectory(String assertionsCsvUploadDirectory) {
        this.assertionsCsvUploadDirectory = assertionsCsvUploadDirectory;
    }

    public String getProcessAssertionUploadsDelay() {
        return processAssertionUploadsDelay;
    }

    public void setProcessAssertionUploadsDelay(String processAssertionUploadsDelay) {
        this.processAssertionUploadsDelay = processAssertionUploadsDelay;
    }

    public String getRemoveStoredFilesDelay() {
        return removeStoredFilesDelay;
    }

    public void setRemoveStoredFilesDelay(String removeStoredFilesDelay) {
        this.removeStoredFilesDelay = removeStoredFilesDelay;
    }
    
    public String getCsvReportsDirectory() {
        return csvReportsDirectory;
    }

    public void setCsvReportsDirectory(String csvReportsDirectory) {
        this.csvReportsDirectory = csvReportsDirectory;
    }

    public String getProcessCsvReportsDelay() {
        return processCsvReportsDelay;
    }

    public void setProcessCsvReportsDelay(String processCsvReportsDelay) {
        this.processCsvReportsDelay = processCsvReportsDelay;
    }
    
    public String getInternalRegistryApiEndpoint() {
        return internalRegistryApiEndpoint;
    }

    public void setInternalRegistryApiEndpoint(String internalRegistryApiEndpoint) {
        this.internalRegistryApiEndpoint = internalRegistryApiEndpoint;
    }
    
    public String getSendPermissionLinkNotificationsDelay() {
        return sendPermissionLinkNotificationsDelay;
    }

    public void setSendPermissionLinkNotificationsDelay(String sendPermissionLinkNotificationsDelay) {
        this.sendPermissionLinkNotificationsDelay = sendPermissionLinkNotificationsDelay;
    }
    
    public int[] getResendNotificationDays() {
        return resendNotificationDays;
    }

    public void setResendNotificationDays(int[] resendNotificationDays) {
        this.resendNotificationDays = resendNotificationDays;
    }
    
    public String getResendNotificationsCron() {
        return resendNotificationsCron;
    }

    public void setResendNotificationsCron(String resendNotificationsCron) {
        this.resendNotificationsCron = resendNotificationsCron;
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


}
