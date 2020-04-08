package org.orcid.user.service;

/**
 * Service containing logic for jhi_users and user_settings.
 * 
 * @author georgenash
 *
 */
public interface UserService {
	
	boolean canCreateUser(String email);

}
