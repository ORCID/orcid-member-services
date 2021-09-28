package org.orcid.memberportal.service.assertion.domain;

import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Field;

public class OrcidToken {

    @Field("salesforce_id")
    private String salesforceId;
    
    @Field("token_id")
    private String tokenId;
    
    @Field("denied_date")
    private Instant deniedDate;
    
    @Field("revoked_date")
    private Instant revokedDate;

    public OrcidToken(final String salesforceId, final String tokenId, final Instant deniedDate, final Instant revokedDate) {
        this.salesforceId = salesforceId;
        this.tokenId = tokenId;
        this.deniedDate = deniedDate;
        this.revokedDate = revokedDate;
    }

    public String getSalesforceId() {
        return this.salesforceId;
    }

    public String getTokenId() {
        return this.tokenId;
    }

    public Instant getDeniedDate() {
        return this.deniedDate;
    }

    public Instant getRevokedDate() {
        return this.revokedDate;
    }
}
