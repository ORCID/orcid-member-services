package org.orcid.user.service.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class UserCaches {
	
	public static final String USERS_BY_EMAIL_CACHE = "usersByEmail";    
	
	@CacheEvict(value = USERS_BY_EMAIL_CACHE, key = "#key")
	public void evictEntryFromEmailCache(String key) {
		// annotation does the work
	}

}
