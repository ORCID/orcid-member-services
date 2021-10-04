package org.orcid.memberportal.service.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private String mailDomain;

    private String mailApiKey;

    private String mailApiUrl;

    private String mailFromAddress;

    private String mailFromName;

    private boolean mailTestMode;

    private String baseUrl;

    private String sendActivationRemindersDelay;
    
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getMailApiUrl() {
        return mailApiUrl;
    }

    public void setMailApiUrl(String mailApiUrl) {
        this.mailApiUrl = mailApiUrl;
    }

    public boolean isMailTestMode() {
        return mailTestMode;
    }

    public void setMailTestMode(boolean mailTestMode) {
        this.mailTestMode = mailTestMode;
    }

    public String getSendActivationRemindersDelay() {
        return sendActivationRemindersDelay;
    }

    public void setSendActivationRemindersDelay(String sendActivationRemindersDelay) {
        this.sendActivationRemindersDelay = sendActivationRemindersDelay;
    }
    
}
