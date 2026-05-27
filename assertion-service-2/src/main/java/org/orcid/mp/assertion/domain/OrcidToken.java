package org.orcid.mp.assertion.domain;

import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

public class OrcidToken {

    @Field("salesforce_id")
    private String salesforceId;

    @Field("member_id")
    private final String memberId;

    @Field("token_id")
    private final String tokenId;

    @Field("denied_date")
    private Instant deniedDate;

    @Field("revoked_date")
    private Instant revokedDate;

    public OrcidToken(final String memberId, final String tokenId) {
        this.memberId = memberId;
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

    public String getMemberId() {
        return memberId;
    }
}
