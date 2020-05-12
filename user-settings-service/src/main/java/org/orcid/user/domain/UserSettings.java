package org.orcid.user.domain;

import java.io.Serializable;
import java.time.Instant;

import org.orcid.user.service.dto.UserDTO;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((deleted == null) ? 0 : deleted.hashCode());
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
        if (deleted == null) {
            if (other.deleted != null)
                return false;
        } else if (!deleted.equals(other.deleted))
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
        return "UserSettings [id=" + id + ", jhiUserId=" + jhiUserId + ", salesforceId=" + salesforceId + ", mainContact=" + mainContact + ", createdBy=" + createdBy
                + ", createdDate=" + createdDate + ", lastModifiedBy=" + lastModifiedBy + ", lastModifiedDate=" + lastModifiedDate + ", deleted=" + deleted + "]";
    }
    
    public static UserSettings valueOf(UserDTO userDTO) {
        UserSettings us = new UserSettings();
        us.setCreatedBy(userDTO.getCreatedBy());
        us.setCreatedDate(userDTO.getCreatedDate());
        us.setDeleted(userDTO.getDeleted());
        us.setId(userDTO.getId());
        us.setJhiUserId(userDTO.getJhiUserId());
        us.setLastModifiedBy(userDTO.getLastModifiedBy());
        us.setLastModifiedDate(userDTO.getLastModifiedDate());
        us.setMainContact(userDTO.getMainContact());
        us.setSalesforceId(userDTO.getSalesforceId());        
        return us;
    }
}
