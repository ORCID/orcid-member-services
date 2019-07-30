package org.orcid.user.domain;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Document;
import javax.validation.constraints.*;

import java.io.Serializable;

/**
 * A MemberServicesUser.
 */
@Document(collection = "member_services_user")
public class MemberServicesUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    
    @Field("user_id")
    private String userId;

    @Field("salesforce_id")
    private String salesforceId;

    @Field("parent_salesforce_id")
    private String parentSalesforceId;

    @Field("disabled")
    private Boolean disabled;

    @Field("main_contact")
    private Boolean mainContact;

    @Field("assertion_service_enabled")
    private Boolean assertionServiceEnabled;
    
    @Field("obo_client_id")
    private String oboClientId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getSalesforceId() {
		return salesforceId;
	}

	public void setSalesforceId(String salesforceId) {
		this.salesforceId = salesforceId;
	}

	public String getParentSalesforceId() {
		return parentSalesforceId;
	}

	public void setParentSalesforceId(String parentSalesforceId) {
		this.parentSalesforceId = parentSalesforceId;
	}

	public Boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public Boolean isMainContact() {
		return mainContact;
	}

	public void setMainContact(Boolean mainContact) {
		this.mainContact = mainContact;
	}

	public Boolean isAssertionServiceEnabled() {
		return assertionServiceEnabled;
	}

	public void setAssertionServiceEnabled(Boolean assertionServiceEnabled) {
		this.assertionServiceEnabled = assertionServiceEnabled;
	}

	public String getOboClientId() {
		return oboClientId;
	}

	public void setOboClientId(String oboClientId) {
		this.oboClientId = oboClientId;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assertionServiceEnabled == null) ? 0 : assertionServiceEnabled.hashCode());
		result = prime * result + ((disabled == null) ? 0 : disabled.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((mainContact == null) ? 0 : mainContact.hashCode());
		result = prime * result + ((oboClientId == null) ? 0 : oboClientId.hashCode());
		result = prime * result + ((parentSalesforceId == null) ? 0 : parentSalesforceId.hashCode());
		result = prime * result + ((salesforceId == null) ? 0 : salesforceId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		MemberServicesUser other = (MemberServicesUser) obj;
		if (assertionServiceEnabled == null) {
			if (other.assertionServiceEnabled != null)
				return false;
		} else if (!assertionServiceEnabled.equals(other.assertionServiceEnabled))
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
		if (mainContact == null) {
			if (other.mainContact != null)
				return false;
		} else if (!mainContact.equals(other.mainContact))
			return false;
		if (oboClientId == null) {
			if (other.oboClientId != null)
				return false;
		} else if (!oboClientId.equals(other.oboClientId))
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
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "MemberServicesUser [id=" + id + ", userId=" + userId + ", salesforceId=" + salesforceId
				+ ", parentSalesforceId=" + parentSalesforceId + ", disabled=" + disabled + ", mainContact="
				+ mainContact + ", assertionServiceEnabled=" + assertionServiceEnabled + ", oboClientId=" + oboClientId
				+ "]";
	}
}
