package org.orcid.auth.service.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orcid.auth.Oauth2ServiceApp;
import org.orcid.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@SpringBootTest(classes = Oauth2ServiceApp.class)
class UserCachesTest {

	@Autowired
	private CacheManager cacheManager;
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserCaches userCaches;

	@BeforeEach
	public void setUp() throws IOException {
		Cache emailCache = cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE);
		Cache loginCache = cacheManager.getCache(UserCaches.USERS_BY_LOGIN_CACHE);

		emailCache.put("test", "value");
		loginCache.put("test", "value");
	}

	@Test
	public void testEvictEntryFromUserCaches() {
		assertNotNull(cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE).get("test"));
		assertNotNull(cacheManager.getCache(UserCaches.USERS_BY_LOGIN_CACHE).get("test"));
		assertEquals("value", cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE).get("test").get());
		assertEquals("value", cacheManager.getCache(UserCaches.USERS_BY_LOGIN_CACHE).get("test").get());
		userCaches.evictEntryFromUserCaches("test");
		assertNull(cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE).get("test"));
		assertNull(cacheManager.getCache(UserCaches.USERS_BY_LOGIN_CACHE).get("test"));
		assertFalse(userRepository.findOneByEmailIgnoreCase("test").isPresent());
		assertFalse(userRepository.findOneByLogin("test").isPresent());
	}

	@Test
	public void testEvictEntryFromLoginCache() {
		assertNotNull(cacheManager.getCache(UserCaches.USERS_BY_LOGIN_CACHE).get("test"));
		assertEquals("value", cacheManager.getCache(UserCaches.USERS_BY_LOGIN_CACHE).get("test").get());
		userCaches.evictEntryFromLoginCache("test");
		assertNull(cacheManager.getCache(UserCaches.USERS_BY_LOGIN_CACHE).get("test"));
		assertFalse(userRepository.findOneByLogin("test").isPresent());
	}

	@Test
	public void testEvictEntryFromEmailCache() {
		assertNotNull(cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE).get("test"));
		assertEquals("value", cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE).get("test").get());
		userCaches.evictEntryFromEmailCache("test");
		assertNull(cacheManager.getCache(UserCaches.USERS_BY_EMAIL_CACHE).get("test"));
		assertFalse(userRepository.findOneByEmailIgnoreCase("test").isPresent());
	}

}
