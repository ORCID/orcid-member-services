package org.orcid.mp.member.apicreds;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApiClientDetails {

    private String memberId;
    private String clientDetailsId;
    private Set<ApiClientRedirectUri> redirectUris = new HashSet<>();
    private String name;
    private String description;
    private boolean allowAutoDeprecate = false;
    private String website;
    private boolean allowMemberOBO = false;
    private boolean allowUserOBO = false;
    private boolean allowResearcherConnect = false;
    private String membershipType;
    private String decryptedSecret;
    private String authenticationProviderId;
    private List<String> errors = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAllowAutoDeprecate() {
        return allowAutoDeprecate;
    }

    public void setAllowAutoDeprecate(boolean allowAutoDeprecate) {
        this.allowAutoDeprecate = allowAutoDeprecate;
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

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
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

    public String getClientDetailsId() {
        return clientDetailsId;
    }

    public void setClientDetailsId(String clientDetailsId) {
        this.clientDetailsId = clientDetailsId;
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