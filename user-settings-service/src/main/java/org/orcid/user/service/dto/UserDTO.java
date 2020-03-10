package org.orcid.user.service.dto;

import java.io.Serializable;
import java.time.Instant;

import org.orcid.user.domain.UserSettings;

public class UserDTO implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -9077279756163937807L;

    // UserSettings data
    private String id;
    private String login;
    private String loginError;
    private String password;
    private String firstName;
    private String firstNameError;
    private String lastName;
    private String lastNameError;        
    private Boolean mainContact;
    private Boolean assertionServicesEnabled;

    // MemberSettings data
    private String salesforceId;
    private String salesforceIdError;
    
    // Metadata
    private String jhiUserId;
    private String createdBy;
    private Instant createdDate;
    private String lastModifiedBy;
    private Instant lastModifiedDate;    

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
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

    public String getLoginError() {
        return loginError;
    }

    public void setLoginError(String loginError) {
        this.loginError = loginError;
    }

    public String getFirstNameError() {
        return firstNameError;
    }

    public void setFirstNameError(String firstNameError) {
        this.firstNameError = firstNameError;
    }

    public String getLastNameError() {
        return lastNameError;
    }

    public void setLastNameError(String lastNameError) {
        this.lastNameError = lastNameError;
    }

    public String getSalesforceIdError() {
        return salesforceIdError;
    }

    public void setSalesforceIdError(String salesforceIdError) {
        this.salesforceIdError = salesforceIdError;
    }

    public Boolean getAssertionServicesEnabled() {
        return assertionServicesEnabled;
    }

    public void setAssertionServicesEnabled(Boolean assertionServicesEnabled) {
        this.assertionServicesEnabled = assertionServicesEnabled;
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
        result = prime * result + ((assertionServicesEnabled == null) ? 0 : assertionServicesEnabled.hashCode());
        result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + ((firstNameError == null) ? 0 : firstNameError.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((jhiUserId == null) ? 0 : jhiUserId.hashCode());
        result = prime * result + ((lastModifiedBy == null) ? 0 : lastModifiedBy.hashCode());
        result = prime * result + ((lastModifiedDate == null) ? 0 : lastModifiedDate.hashCode());
        result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
        result = prime * result + ((lastNameError == null) ? 0 : lastNameError.hashCode());
        result = prime * result + ((login == null) ? 0 : login.hashCode());
        result = prime * result + ((loginError == null) ? 0 : loginError.hashCode());
        result = prime * result + ((mainContact == null) ? 0 : mainContact.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((salesforceId == null) ? 0 : salesforceId.hashCode());
        result = prime * result + ((salesforceIdError == null) ? 0 : salesforceIdError.hashCode());
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
        UserDTO other = (UserDTO) obj;
        if (assertionServicesEnabled == null) {
            if (other.assertionServicesEnabled != null)
                return false;
        } else if (!assertionServicesEnabled.equals(other.assertionServicesEnabled))
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
        if (firstName == null) {
            if (other.firstName != null)
                return false;
        } else if (!firstName.equals(other.firstName))
            return false;
        if (firstNameError == null) {
            if (other.firstNameError != null)
                return false;
        } else if (!firstNameError.equals(other.firstNameError))
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
        if (lastName == null) {
            if (other.lastName != null)
                return false;
        } else if (!lastName.equals(other.lastName))
            return false;
        if (lastNameError == null) {
            if (other.lastNameError != null)
                return false;
        } else if (!lastNameError.equals(other.lastNameError))
            return false;
        if (login == null) {
            if (other.login != null)
                return false;
        } else if (!login.equals(other.login))
            return false;
        if (loginError == null) {
            if (other.loginError != null)
                return false;
        } else if (!loginError.equals(other.loginError))
            return false;
        if (mainContact == null) {
            if (other.mainContact != null)
                return false;
        } else if (!mainContact.equals(other.mainContact))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        if (salesforceId == null) {
            if (other.salesforceId != null)
                return false;
        } else if (!salesforceId.equals(other.salesforceId))
            return false;
        if (salesforceIdError == null) {
            if (other.salesforceIdError != null)
                return false;
        } else if (!salesforceIdError.equals(other.salesforceIdError))
            return false;
        return true;
    }

    public static UserDTO valueOf(UserSettings us) {
        UserDTO result = new UserDTO();
        result.setId(us.getId());
        result.setJhiUserId(us.getJhiUserId());
        result.setMainContact(us.getMainContact());
        result.setSalesforceId(us.getSalesforceId());
        result.setCreatedBy(us.getCreatedBy());
        result.setCreatedDate(us.getCreatedDate());
        result.setLastModifiedBy(us.getLastModifiedBy());
        result.setLastModifiedDate(us.getLastModifiedDate());        
        return result;
    }

    @Override
    public String toString() {
        return "UserDTO [id=" + id + ", login=" + login + ", loginError=" + loginError + ", password=" + password + ", firstName=" + firstName + ", firstNameError="
                + firstNameError + ", lastName=" + lastName + ", lastNameError=" + lastNameError + ", mainContact=" + mainContact + ", assertionServicesEnabled="
                + assertionServicesEnabled + ", salesforceId=" + salesforceId + ", salesforceIdError=" + salesforceIdError + ", jhiUserId=" + jhiUserId + ", createdBy="
                + createdBy + ", createdDate=" + createdDate + ", lastModifiedBy=" + lastModifiedBy + ", lastModifiedDate=" + lastModifiedDate + "]";
    }    
}
