package org.orcid.memberportal.service.gateway.security.oauth2;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

public class LoginResult {
    
    private boolean mfaRequired;
    
    private OAuth2AccessToken oauth2AccessToken;

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public void setMfaRequired(boolean mfaRequired) {
        this.mfaRequired = mfaRequired;
    }

    public OAuth2AccessToken getOauth2AccessToken() {
        return oauth2AccessToken;
    }

    public void setOauth2AccessToken(OAuth2AccessToken oauth2AccessToken) {
        this.oauth2AccessToken = oauth2AccessToken;
    }
    
}
