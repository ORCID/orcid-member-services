package org.orcid.user.service.dto;

import java.util.List;

import org.orcid.user.domain.UserSettings;

public class UserDTO extends UserSettings {
    /**
     * 
     */
    private static final long serialVersionUID = 6064146099241309846L;
    private String login;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> authorities;

    public List<String> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<String> authorities) {
        this.authorities = authorities;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((authorities == null) ? 0 : authorities.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((firstName == null) ? 0 : firstName.hashCode());
        result = prime * result + ((lastName == null) ? 0 : lastName.hashCode());
        result = prime * result + ((login == null) ? 0 : login.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        UserDTO other = (UserDTO) obj;
        if (authorities == null) {
            if (other.authorities != null)
                return false;
        } else if (!authorities.equals(other.authorities))
            return false;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (firstName == null) {
            if (other.firstName != null)
                return false;
        } else if (!firstName.equals(other.firstName))
            return false;
        if (lastName == null) {
            if (other.lastName != null)
                return false;
        } else if (!lastName.equals(other.lastName))
            return false;
        if (login == null) {
            if (other.login != null)
                return false;
        } else if (!login.equals(other.login))
            return false;
        if (password == null) {
            if (other.password != null)
                return false;
        } else if (!password.equals(other.password))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "UserDTO [login=" + login + ", password=" + password + ", firstName=" + firstName + ", lastName=" + lastName + ", email=" + email + ", authorities="
                + authorities + "]";
    }
    
    public static UserDTO valueOf(UserSettings us) {
        UserDTO result = new UserDTO();
        result.setCreatedBy(us.getCreatedBy());
        result.setCreatedDate(us.getCreatedDate());
        result.setDisabled(us.getDisabled());               
        result.setId(us.getId());
        result.setJhiUserId(us.getJhiUserId());
        result.setLastModifiedBy(us.getLastModifiedBy());
        result.setLastModifiedDate(us.getLastModifiedDate());
        result.setMainContact(us.getMainContact());
        result.setMemberId(us.getMemberId());
        result.setSalesforceId(us.getSalesforceId());
        return result;
    }

}
