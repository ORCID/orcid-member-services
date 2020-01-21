package org.orcid.user.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
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

    @Field("client_id")
    private String clientId;

    @NotNull
    @Field("salesforce_id")
    private String salesforceId;

    @Field("parent_salesforce_id")
    private String parentSalesforceId;

    @Field("client_name")
    private String clientName;

    @NotNull
    @Field("is_consortium_lead")
    private Boolean isConsortiumLead;

    @Field("assertion_service_enabled")
    private Boolean assertionServiceEnabled;

    @Field("created_by")
    private String createdBy;

    @Field("created_date")
    private Instant createdDate;

    @Field("last_modified_by")
    private String lastModifiedBy;

    @Field("last_modified_date")
    private Instant lastModifiedDate;

    @Transient
    private String error;

    // jhipster-needle-entity-add-field - JHipster will add fields here, do not
    // remove
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

    public Boolean getIsConsortiumLead() {
        return isConsortiumLead;
    }

    public void setIsConsortiumLead(Boolean isConsortiumLead) {
        this.isConsortiumLead = isConsortiumLead;
    }

    public Boolean getAssertionServiceEnabled() {
        return assertionServiceEnabled;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((assertionServiceEnabled == null) ? 0 : assertionServiceEnabled.hashCode());
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result + ((clientName == null) ? 0 : clientName.hashCode());
        result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((error == null) ? 0 : error.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((isConsortiumLead == null) ? 0 : isConsortiumLead.hashCode());
        result = prime * result + ((lastModifiedBy == null) ? 0 : lastModifiedBy.hashCode());
        result = prime * result + ((lastModifiedDate == null) ? 0 : lastModifiedDate.hashCode());
        result = prime * result + ((parentSalesforceId == null) ? 0 : parentSalesforceId.hashCode());
        result = prime * result + ((salesforceId == null) ? 0 : salesforceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MemberSettings other = (MemberSettings) obj;
        if (assertionServiceEnabled == null) {
            if (other.assertionServiceEnabled != null)
                return false;
        } else if (!assertionServiceEnabled.equals(other.assertionServiceEnabled))
            return false;
        if (clientId == null) {
            if (other.clientId != null)
                return false;
        } else if (!clientId.equals(other.clientId))
            return false;
        if (clientName == null) {
            if (other.clientName != null)
                return false;
        } else if (!clientName.equals(other.clientName))
            return false;
        if (createdBy == null) {
            if (other.createdBy != null)
                return false;
        } else if (!createdBy.equals(other.createdBy))
            return false;
        if (createdDate == null) {
            if (other.createdDate != null)
                return false;
        } else if (!createdDate.equals(other.createdDate))
            return false;
        if (error == null) {
            if (other.error != null)
                return false;
        } else if (!error.equals(other.error))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (isConsortiumLead == null) {
            if (other.isConsortiumLead != null)
                return false;
        } else if (!isConsortiumLead.equals(other.isConsortiumLead))
            return false;
        if (lastModifiedBy == null) {
            if (other.lastModifiedBy != null)
                return false;
        } else if (!lastModifiedBy.equals(other.lastModifiedBy))
            return false;
        if (lastModifiedDate == null) {
            if (other.lastModifiedDate != null)
                return false;
        } else if (!lastModifiedDate.equals(other.lastModifiedDate))
            return false;
        if (parentSalesforceId == null) {
            if (other.parentSalesforceId != null)
                return false;
        } else if (!parentSalesforceId.equals(other.parentSalesforceId))
            return false;
        if (salesforceId == null) {
            if (other.salesforceId != null)
                return false;
        } else if (!salesforceId.equals(other.salesforceId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "MemberSettings [id=" + id + ", clientId=" + clientId + ", salesforceId=" + salesforceId + ", parentSalesforceId=" + parentSalesforceId + ", clientName="
                + clientName + ", isConsortiumLead=" + isConsortiumLead + ", assertionServiceEnabled=" + assertionServiceEnabled + ", createdBy=" + createdBy
                + ", createdDate=" + createdDate + ", lastModifiedBy=" + lastModifiedBy + ", lastModifiedDate=" + lastModifiedDate + ", error=" + error + "]";
    }

}
