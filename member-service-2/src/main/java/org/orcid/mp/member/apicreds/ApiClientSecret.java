package org.orcid.mp.member.apicreds;

public class ApiClientSecret {

    private String clientSecret;

    public ApiClientSecret() {
    }

    public ApiClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
