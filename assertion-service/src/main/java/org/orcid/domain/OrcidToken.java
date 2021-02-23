package org.orcid.domain;

import java.time.Instant;

public class OrcidToken {
    
    private String salesforce_id;
    private String token_id;
    private Instant denied_date;
    private Instant revoked_date;
    
    public OrcidToken( final String salesforce_id, final String token_id, final Instant denied_date, final Instant revoked_date) {
        this.salesforce_id = salesforce_id;
        this.token_id = token_id;
        this.denied_date = denied_date;
        this.revoked_date = revoked_date;
    }

    public String getSalesforce_id() {
        return this.salesforce_id;
    }

    public String getToken_id() {
        return this.token_id;
    }
    
    public Instant getDenied_date() {
    	return this.denied_date;
    }
    
    public Instant getRevoked_date() {
    	return this.revoked_date;
    }
}
