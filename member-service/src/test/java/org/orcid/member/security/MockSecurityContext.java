package org.orcid.member.security;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;

public class MockSecurityContext implements SecurityContext {
	
	private static final long serialVersionUID = 1L;

	private String loggedInUserName;
	
	public MockSecurityContext(String loggedInUsername) {
		this.loggedInUserName = loggedInUsername;
	}

	@Override
	public Authentication getAuthentication() {
		return new Authentication() {
			
			private static final long serialVersionUID = 1L;

			@Override
			public String getName() {
				return loggedInUserName;
			}
			
			@Override
			public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
			}
			
			@Override
			public boolean isAuthenticated() {
				return true;
			}
			
			@Override
			public Object getPrincipal() {
				return loggedInUserName;
			}
			
			@Override
			public Object getDetails() {
				return null;
			}
			
			@Override
			public Object getCredentials() {
				return null;
			}
			
			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return null;
			}
		};
	}

	@Override
	public void setAuthentication(Authentication authentication) {
	}

}
