package org.orcid.mp.member.domain;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.Instant;

@Document(collection = "member")
public class Member implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field("client_id")
    private String clientId;

    @NotNull
    @Indexed(unique = true)
    @Field("salesforce_id")
    private String salesforceId;

    @Field("parent_salesforce_id")
    private String parentSalesforceId;

    @Field("client_name")
    private String clientName;

    @NotNull
    @Field("is_consortium_lead")
    private Boolean isConsortiumLead;

    @NotNull
    @Field("assertion_service_enabled")
    private Boolean assertionServiceEnabled;

    @Field("superadmin_enabled")
    private Boolean superadminEnabled;

    @Field("created_by")
    private String createdBy;

    @Field("created_date")
    private Instant createdDate;

    @Field("last_modified_by")
    private String lastModifiedBy;

    @Field("last_modified_date")
    private Instant lastModifiedDate;

    @Field("default_language")
    private String defaultLanguage;

    @Field
    private boolean active;

    @Field("last_updated_with_salesforce_data")
    private Instant lastUpdatedWithSalesforceData;

    @Field("deactivated_date")
    private Instant deactivatedDate;

    @Field("activated_date")
    private Instant activatedDate;

    @Field
    private String type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id.trim() : id;
    }

    public String getClientId() {
        return clientId != null ? clientId.trim() : null;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId != null ? clientId.trim() : null;
    }

    public String getSalesforceId() {
        return salesforceId != null ? salesforceId.trim() : null;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId != null ? salesforceId.trim() : null;
    }

    public String getParentSalesforceId() {
        return parentSalesforceId != null ? parentSalesforceId.trim() : null;
    }

    public void setParentSalesforceId(String parentSalesforceId) {
        this.parentSalesforceId = parentSalesforceId != null ? parentSalesforceId.trim() : null;
    }

    public Boolean isAssertionServiceEnabled() {
        return assertionServiceEnabled;
    }

    public void setAssertionServiceEnabled(Boolean assertionServiceEnabled) {
        this.assertionServiceEnabled = assertionServiceEnabled;
    }

    public String getCreatedBy() {
        return createdBy != null ? createdBy.trim() : null;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy != null ? createdBy.trim() : null;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy != null ? lastModifiedBy.trim() : lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy != null ? lastModifiedBy.trim() : lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
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

    public Boolean getSuperadminEnabled() {
        return superadminEnabled;
    }

    public void setSuperadminEnabled(Boolean superadminEnabled) {
        this.superadminEnabled = superadminEnabled;
    }

    public String getClientName() {
        return clientName != null ? clientName.trim() : null;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName != null ? clientName.trim() : null;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastUpdatedWithSalesforceData() {
        return lastUpdatedWithSalesforceData;
    }

    public void setLastUpdatedWithSalesforceData(Instant lastUpdatedWithSalesforceData) {
        this.lastUpdatedWithSalesforceData = lastUpdatedWithSalesforceData;
    }

    public Instant getDeactivatedDate() {
        return deactivatedDate;
    }

    public void setDeactivatedDate(Instant deactivatedDate) {
        this.deactivatedDate = deactivatedDate;
    }

    public Instant getActivatedDate() {
        return activatedDate;
    }

    public void setActivatedDate(Instant activatedDate) {
        this.activatedDate = activatedDate;
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
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((isConsortiumLead == null) ? 0 : isConsortiumLead.hashCode());
        result = prime * result + ((superadminEnabled == null) ? 0 : superadminEnabled.hashCode());
        result = prime * result + ((lastModifiedBy == null) ? 0 : lastModifiedBy.hashCode());
        result = prime * result + ((lastModifiedDate == null) ? 0 : lastModifiedDate.hashCode());
        result = prime * result + ((parentSalesforceId == null) ? 0 : parentSalesforceId.hashCode());
        result = prime * result + ((salesforceId == null) ? 0 : salesforceId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((defaultLanguage == null) ? 0 : defaultLanguage.hashCode());
        result = prime * result + ((deactivatedDate == null) ? 0 : deactivatedDate.hashCode());
        result = prime * result + ((activatedDate == null) ? 0 : activatedDate.hashCode());
        result = prime * result + ((lastUpdatedWithSalesforceData == null) ? 0 : lastUpdatedWithSalesforceData.hashCode());
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
        Member other = (Member) obj;
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
        if (superadminEnabled == null) {
            if (other.superadminEnabled != null)
                return false;
        } else if (!superadminEnabled.equals(other.superadminEnabled))
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
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (defaultLanguage == null) {
            if (other.defaultLanguage != null)
                return false;
        } else if (!defaultLanguage.equals(other.defaultLanguage))
            return false;
        if (deactivatedDate == null) {
            if (other.deactivatedDate != null)
                return false;
        } else if (!deactivatedDate.equals(other.deactivatedDate)) {
            return false;
        }
        if (activatedDate == null) {
            if (other.activatedDate != null)
                return false;
        } else if (!activatedDate.equals(other.activatedDate)) {
            return false;
        }
        if (lastUpdatedWithSalesforceData == null) {
            if (other.lastUpdatedWithSalesforceData != null)
                return false;
        } else if (!lastUpdatedWithSalesforceData.equals(other.lastUpdatedWithSalesforceData)) {
            return false;
        }
        if (active != other.active) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Member [id=" + id + ", clientId=" + clientId + ", salesforceId=" + salesforceId + ", parentSalesforceId=" + parentSalesforceId + ", clientName="
                + clientName + ", isConsortiumLead=" + isConsortiumLead + ", assertionServiceEnabled=" + assertionServiceEnabled + ", superadminEnabled="
                + superadminEnabled + ", createdBy=" + createdBy + ", createdDate=" + createdDate + ", lastModifiedBy=" + lastModifiedBy + ", lastModifiedDate="
                + lastModifiedDate + ", type=" + type  + ", defaultLanguage=" + defaultLanguage + ", active=" + active + "]";
    }

}