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

    public OrcidToken(final String salesforceId, final String tokenId) {
        this.salesforceId = salesforceId;
        this.tokenId = tokenId;
    }

    public String getSalesforceId() {
        return this.salesforceId;
    }

    public String getTokenId() {
        return this.tokenId;
    }

    public Instant getDeniedDate() {
        return deniedDate;
    }

    public void setDeniedDate(Instant deniedDate) {
        this.deniedDate = deniedDate;
    }

    public Instant getRevokedDate() {
        return revokedDate;
    }

    public void setRevokedDate(Instant revokedDate) {
        this.revokedDate = revokedDate;
    }

}
