package org.orcid.user.service.dto;

import java.util.List;

import org.orcid.user.domain.MemberServicesUser;

public class MemberServicesUserDTO extends MemberServicesUser {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6064146099241309846L;
	private String login;
	private String email;
	private String password;	
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((authorities == null) ? 0 : authorities.hashCode());
		result = prime * result + ((email == null) ? 0 : email.hashCode());
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
		MemberServicesUserDTO other = (MemberServicesUserDTO) obj;
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
		return "MemberServicesUserDTO [login=" + login + ", email=" + email + ", password=" + password
				+ ", authorities=" + authorities + "]";
	}

	public static MemberServicesUserDTO valueOf(MemberServicesUser msu) {
		MemberServicesUserDTO dto = new MemberServicesUserDTO();
		dto.setAssertionServiceEnabled(msu.isAssertionServiceEnabled());
		dto.setDisabled(msu.isDisabled());		
		dto.setId(msu.getId());
		dto.setMainContact(msu.isMainContact());		
		dto.setParentSalesforceId(msu.getParentSalesforceId());
		dto.setSalesforceId(msu.getSalesforceId());
		dto.setUserId(msu.getUserId());
		return dto;
	}
	
}
