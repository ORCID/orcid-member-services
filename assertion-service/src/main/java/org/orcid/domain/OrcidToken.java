package org.orcid.domain;

import java.time.Instant;

public class OrcidToken {
    
    private String salesforce_id;
    private String token_id;
    private Instant deniedDate;
    
    public OrcidToken( final String salesforce_id, final String token_id, final Instant deniedDate) {
        this.salesforce_id = salesforce_id;
        this.token_id = token_id;
        this.deniedDate = deniedDate;
    }

    public String getSalesforce_id() {
        return this.salesforce_id;
    }

    public String getToken_id() {
        return this.token_id;
    }
    
    public Instant getDeniedDate() {
    	return this.deniedDate;
    }

}
