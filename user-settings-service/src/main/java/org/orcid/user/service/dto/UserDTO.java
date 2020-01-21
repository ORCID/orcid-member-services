package org.orcid.user.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

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
    private List<String> authorities;
    private String authoritiesError;
    private Boolean mainContact;

    // MemberSettings data
    private String salesforceId;
    private String salesforceIdError;
    private String parentSalesforceId;
    private String parentSalesforceIdError;
    private String clientId;
    private String clientIdError;
    private String clientName;
    private String clientNameError;
    private Boolean isConsortiumLead;

    // Metadata
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

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
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

    public Boolean getIsConsortiumLead() {
        return isConsortiumLead;
    }

    public void setIsConsortiumLead(Boolean isConsortiumLead) {
        this.isConsortiumLead = isConsortiumLead;
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

    public String getAuthoritiesError() {
        return authoritiesError;
    }

    public void setAuthoritiesError(String authoritiesError) {
        this.authoritiesError = authoritiesError;
    }

    public String getSalesforceIdError() {
        return salesforceIdError;
    }

    public void setSalesforceIdError(String salesforceIdError) {
        this.salesforceIdError = salesforceIdError;
    }

    public String getParentSalesforceIdError() {
        return parentSalesforceIdError;
    }

    public void setParentSalesforceIdError(String parentSalesforceIdError) {
        this.parentSalesforceIdError = parentSalesforceIdError;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientIdError() {
        return clientIdError;
    }

    public void setClientIdError(String clientIdError) {
        this.clientIdError = clientIdError;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientNameError() {
        return clientNameError;
    }

    public void setClientNameError(String clientNameError) {
        this.clientNameError = clientNameError;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((authorities == null) ? 0 : authorities.hashCode());
        result = prime * result + ((authoritiesError == null) ? 0 : authoritiesError.hashCode());
        result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result + ((clientIdError == null) ? 0 : clientIdError.hashCode());
        result = prime * result + ((clientName == null) ? 0 : clientName.hashCode());
        result = prime * result + ((clientNameError == null) ? 0 : clientNameError.hashCode());
        result = prime * result + ((createdBy == null) ? 0 : createdBy.hashCode());
        result = prime * result + ((createdDate == null) ? 0 : createdDate.hashCode());
        result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + ((firstNameError == null) ? 0 : firstNameError.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((isConsortiumLead == null) ? 0 : isConsortiumLead.hashCode());
        result = prime * result + ((lastModifiedBy == null) ? 0 : lastModifiedBy.hashCode());
        result = prime * result + ((lastModifiedDate == null) ? 0 : lastModifiedDate.hashCode());
        result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
        result = prime * result + ((lastNameError == null) ? 0 : lastNameError.hashCode());
        result = prime * result + ((login == null) ? 0 : login.hashCode());
        result = prime * result + ((loginError == null) ? 0 : loginError.hashCode());
        result = prime * result + ((mainContact == null) ? 0 : mainContact.hashCode());
        result = prime * result + ((parentSalesforceId == null) ? 0 : parentSalesforceId.hashCode());
        result = prime * result + ((parentSalesforceIdError == null) ? 0 : parentSalesforceIdError.hashCode());
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
        if (authorities == null) {
            if (other.authorities != null)
                return false;
        } else if (!authorities.equals(other.authorities))
            return false;
        if (authoritiesError == null) {
            if (other.authoritiesError != null)
                return false;
        } else if (!authoritiesError.equals(other.authoritiesError))
            return false;
        if (clientId == null) {
            if (other.clientId != null)
                return false;
        } else if (!clientId.equals(other.clientId))
            return false;
        if (clientIdError == null) {
            if (other.clientIdError != null)
                return false;
        } else if (!clientIdError.equals(other.clientIdError))
            return false;
        if (clientName == null) {
            if (other.clientName != null)
                return false;
        } else if (!clientName.equals(other.clientName))
            return false;
        if (clientNameError == null) {
            if (other.clientNameError != null)
                return false;
        } else if (!clientNameError.equals(other.clientNameError))
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
        if (parentSalesforceId == null) {
            if (other.parentSalesforceId != null)
                return false;
        } else if (!parentSalesforceId.equals(other.parentSalesforceId))
            return false;
        if (parentSalesforceIdError == null) {
            if (other.parentSalesforceIdError != null)
                return false;
        } else if (!parentSalesforceIdError.equals(other.parentSalesforceIdError))
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

    @Override
    public String toString() {
        return "UserDTO [id=" + id + ", login=" + login + ", loginError=" + loginError + ", password=" + password + ", firstName=" + firstName + ", firstNameError="
                + firstNameError + ", lastName=" + lastName + ", lastNameError=" + lastNameError + ", authorities=" + authorities + ", authoritiesError="
                + authoritiesError + ", mainContact=" + mainContact + ", salesforceId=" + salesforceId + ", salesforceIdError=" + salesforceIdError
                + ", parentSalesforceId=" + parentSalesforceId + ", parentSalesforceIdError=" + parentSalesforceIdError + ", clientId=" + clientId + ", clientIdError="
                + clientIdError + ", clientName=" + clientName + ", clientNameError=" + clientNameError + ", isConsortiumLead=" + isConsortiumLead + ", createdBy="
                + createdBy + ", createdDate=" + createdDate + ", lastModifiedBy=" + lastModifiedBy + ", lastModifiedDate=" + lastModifiedDate + "]";
    }

    public static UserDTO valueOf(UserSettings us) {
        UserDTO result = new UserDTO();
        result.setId(us.getId());
        result.setLogin(us.getLogin());
        result.setMainContact(us.getMainContact());
        result.setSalesforceId(us.getSalesforceId());
        result.setCreatedBy(us.getCreatedBy());
        result.setCreatedDate(us.getCreatedDate());
        result.setLastModifiedBy(us.getLastModifiedBy());
        result.setLastModifiedDate(us.getLastModifiedDate());        
        return result;
    }

}
