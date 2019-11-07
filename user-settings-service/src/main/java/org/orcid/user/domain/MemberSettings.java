package org.orcid.user.domain;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * A MemberSettings.
 */
@Document(collection = "member_settings")
public class MemberSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @NotNull
    @Field("client_id")
    private String clientId;

    @NotNull
    @Field("client_secret")
    private String clientSecret;

    @Field("salesforce_id")
    private String salesforceId;

    @Field("parent_salesforce_id")
    private String parentSalesforceId;

    @Field("assertion_service_enabled")
    private Boolean assertionServiceEnabled;

    @NotNull
    @Field("created_by")
    private String createdBy;

    @NotNull
    @Field("created_date")
    private Instant createdDate;

    @NotNull
    @Field("last_modified_by")
    private String lastModifiedBy;

    @NotNull
    @Field("last_modified_date")
    private Instant lastModifiedDate;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not remove
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public MemberSettings clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public MemberSettings clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getSalesforceId() {
        return salesforceId;
    }

    public MemberSettings salesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
        return this;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }

    public String getParentSalesforceId() {
        return parentSalesforceId;
    }

    public MemberSettings parentSalesforceId(String parentSalesforceId) {
        this.parentSalesforceId = parentSalesforceId;
        return this;
    }

    public void setParentSalesforceId(String parentSalesforceId) {
        this.parentSalesforceId = parentSalesforceId;
    }

    public Boolean isAssertionServiceEnabled() {
        return assertionServiceEnabled;
    }

    public MemberSettings assertionServiceEnabled(Boolean assertionServiceEnabled) {
        this.assertionServiceEnabled = assertionServiceEnabled;
        return this;
    }

    public void setAssertionServiceEnabled(Boolean assertionServiceEnabled) {
        this.assertionServiceEnabled = assertionServiceEnabled;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public MemberSettings createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public MemberSettings createdDate(Instant createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public MemberSettings lastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
        return this;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public MemberSettings lastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
        return this;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here, do not remove

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MemberSettings)) {
            return false;
        }
        return id != null && id.equals(((MemberSettings) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return "MemberSettings{" +
            "id=" + getId() +
            ", clientId='" + getClientId() + "'" +
            ", clientSecret='" + getClientSecret() + "'" +
            ", salesforceId='" + getSalesforceId() + "'" +
            ", parentSalesforceId='" + getParentSalesforceId() + "'" +
            ", assertionServiceEnabled='" + isAssertionServiceEnabled() + "'" +
            ", createdBy='" + getCreatedBy() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", lastModifiedBy='" + getLastModifiedBy() + "'" +
            ", lastModifiedDate='" + getLastModifiedDate() + "'" +
            "}";
    }
}
