package org.orcid.user.web.rest.errors;

public class MemberNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public MemberNotFoundException(String message) {
		super(message);
	}

}
