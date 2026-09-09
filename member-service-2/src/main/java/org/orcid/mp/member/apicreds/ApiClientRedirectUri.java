package org.orcid.mp.member.apicreds;


import java.util.ArrayList;
import java.util.List;

public class ApiClientRedirectUri {

    private String redirectUri;
    private String redirectUriType = "default";
    private String predefinedClientRedirectScope;

    private List<String> errors = new ArrayList<>();
    private List<String> scopes = new ArrayList<>();

    public ApiClientRedirectUri() {
    }

    public ApiClientRedirectUri(String redirectUri, String redirectUriType, String predefinedClientRedirectScope) {
        this.redirectUri = redirectUri;
        this.redirectUriType = redirectUriType;
        this.predefinedClientRedirectScope = predefinedClientRedirectScope;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getRedirectUriType() {
        return redirectUriType;
    }

    public void setRedirectUriType(String redirectUriType) {
        this.redirectUriType = redirectUriType;
    }

    public String getPredefinedClientRedirectScope() {
        return predefinedClientRedirectScope;
    }

    public void setPredefinedClientRedirectScope(String predefinedClientRedirectScope) {
        this.predefinedClientRedirectScope = predefinedClientRedirectScope;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}
