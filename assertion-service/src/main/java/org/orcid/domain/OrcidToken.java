package org.orcid.domain;

public class OrcidToken {
    
    private String salesforce_id;
    private String token_id;
    
    public OrcidToken( final String salesforce_id, final String token_id) {
        this.salesforce_id = salesforce_id;
        this.token_id = token_id;
    }

    public String getSalesforce_id() {
        return this.salesforce_id;
    }

    public String getToken_id() {
        return this.token_id;
    }

}
