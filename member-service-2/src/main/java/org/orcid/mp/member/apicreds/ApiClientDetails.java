package org.orcid.mp.member.apicreds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApiClientDetails extends ApiClientSummary {

    private Set<ApiClientRedirectUri> redirectUris = new HashSet<>();
    private String description;
    private String website;
    private boolean allowMemberOBO = false;
    private boolean allowUserOBO = false;
    private boolean allowResearcherConnect = false;
    private String membershipType;
    private String decryptedSecret;
    private String authenticationProviderId;
    private List<String> errors = new ArrayList<>();

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public boolean isAllowMemberOBO() {
        return allowMemberOBO;
    }

    public void setAllowMemberOBO(boolean allowMemberOBO) {
        this.allowMemberOBO = allowMemberOBO;
    }

    public boolean isAllowUserOBO() {
        return allowUserOBO;
    }

    public void setAllowUserOBO(boolean allowUserOBO) {
        this.allowUserOBO = allowUserOBO;
    }

    public boolean isAllowResearcherConnect() {
        return allowResearcherConnect;
    }

    public void setAllowResearcherConnect(boolean allowResearcherConnect) {
        this.allowResearcherConnect = allowResearcherConnect;
    }

    public Set<ApiClientRedirectUri> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<ApiClientRedirectUri> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getMembershipType() {
        return membershipType;
    }

    public void setMembershipType(String membershipType) {
        this.membershipType = membershipType;
    }

    public String getDecryptedSecret() {
        return decryptedSecret;
    }

    public void setDecryptedSecret(String decryptedSecret) {
        this.decryptedSecret = decryptedSecret;
    }

    public String getAuthenticationProviderId() {
        return authenticationProviderId;
    }

    public void setAuthenticationProviderId(String authenticationProviderId) {
        this.authenticationProviderId = authenticationProviderId;
    }
}