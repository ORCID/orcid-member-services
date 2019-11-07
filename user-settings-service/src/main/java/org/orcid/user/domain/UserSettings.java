package org.orcid.user.domain;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

import javax.validation.constraints.NotNull;

@Document(collection = "user_settings")
public class UserSettings implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -4167368782427296692L;

    @Id
    private String id;
    
    @Field("jhi_user_id")
    private String jhiUserId;

    @Field("salesforce_id")
    private String salesforceId;

    @Field("disabled")
    private Boolean disabled;

    @Field("main_contact")
    private Boolean mainContact;

    @Field("created_by")
    private String createdBy;

    @Field("created_date")
    private Instant createdDate;

    @Field("last_modified_by")
    private String lastModifiedBy;

    @Field("last_modified_date")
    private Instant lastModifiedDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJhiUserId() {
        return jhiUserId;
    }

    public void setJhiUserId(String jhiUserId) {
        this.jhiUserId = jhiUserId;
    }

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }

   public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public Boolean getMainContact() {
        return mainContact;
    }

    public void setMainContact(Boolean mainContact) {
        this.mainContact = mainContact;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((disabled == null) ? 0 : disabled.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((jhiUserId == null) ? 0 : jhiUserId.hashCode());
        result = prime * result + ((lastModifiedBy == null) ? 0 : lastModifiedBy.hashCode());
        result = prime * result + ((lastModifiedDate == null) ? 0 : lastModifiedDate.hashCode());
        result = prime * result + ((mainContact == null) ? 0 : mainContact.hashCode());        
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
        UserSettings other = (UserSettings) obj;
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
        if (disabled == null) {
            if (other.disabled != null)
                return false;
        } else if (!disabled.equals(other.disabled))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (jhiUserId == null) {
            if (other.jhiUserId != null)
                return false;
        } else if (!jhiUserId.equals(other.jhiUserId))
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
        if (mainContact == null) {
            if (other.mainContact != null)
                return false;
        } else if (!mainContact.equals(other.mainContact))
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
        return "UserSettings [id=" + id + ", jhiUserId=" + jhiUserId + ", salesforceId=" + salesforceId + ", disabled=" + disabled
                + ", mainContact=" + mainContact + ", createdBy=" + createdBy + ", createdDate=" + createdDate + ", lastModifiedBy=" + lastModifiedBy
                + ", lastModifiedDate=" + lastModifiedDate + "]";
    }

}
