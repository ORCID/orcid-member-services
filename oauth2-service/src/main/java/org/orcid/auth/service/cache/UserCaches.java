package org.orcid.auth.service.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class UserCaches {
	
	public static final String USERS_BY_LOGIN_CACHE = "usersByLogin";

	public static final String USERS_BY_EMAIL_CACHE = "usersByEmail";    
	
	@CacheEvict(value = { USERS_BY_LOGIN_CACHE, USERS_BY_EMAIL_CACHE }, key = "#key")
	public void evictEntryFromUserCaches(String key) {
	}
	
	@CacheEvict(value = USERS_BY_LOGIN_CACHE, key = "#key")
	public void evictEntryFromLoginCache(String key) {
		// annotation does the work
	}
	
	@CacheEvict(value = USERS_BY_EMAIL_CACHE, key = "#key")
	public void evictEntryFromEmailCache(String key) {
		// annotation does the work
	}

}
