package org.orcid.auth.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.orcid.auth.config.Constants;
import org.orcid.auth.domain.Authority;
import org.orcid.auth.domain.Member;
import org.orcid.auth.domain.User;
import org.orcid.auth.repository.AuthorityRepository;
import org.orcid.auth.repository.MemberRepository;
import org.orcid.auth.repository.UserRepository;
import org.orcid.auth.security.AuthoritiesConstants;
import org.orcid.auth.security.SecurityUtils;
import org.orcid.auth.service.cache.UserCaches;
import org.orcid.auth.service.dto.UserDTO;
import org.orcid.auth.service.util.RandomUtil;
import org.orcid.auth.web.rest.errors.BadRequestAlertException;
import org.orcid.auth.web.rest.errors.EmailAlreadyUsedException;
import org.orcid.auth.web.rest.errors.InvalidPasswordException;
import org.orcid.auth.web.rest.errors.LoginAlreadyUsedException;
import org.orcid.auth.web.rest.errors.MemberNotFoundException;
import org.orcid.user.upload.MembersUploadReader;
import org.orcid.user.upload.UsersUpload;
import org.orcid.user.upload.UsersUploadReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service class for managing users.
 */
@Service
public class UserService {

	private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthorityRepository authorityRepository;

	@Autowired
	private UserCaches userCaches;

	@Autowired
	private UsersUploadReader usersUploadReader;

	@Autowired
	private MembersUploadReader membersUploadReader;

	public Optional<User> activateRegistration(String key) {
		LOG.debug("Activating user for activation key {}", key);
		return userRepository.findOneByActivationKey(key).map(user -> {
			// activate given user for the registration key.
			user.setActivated(true);
			user.setActivationKey(null);
			userRepository.save(user);
			userCaches.evictEntryFromUserCaches(user.getEmail());
			LOG.debug("Activated user: {}", user);
			return user;
		});
	}

	public Optional<User> completePasswordReset(String newPassword, String key) {
		LOG.debug("Reset user password for reset key {}", key);
		return userRepository.findOneByResetKey(key)
				.filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400))).map(user -> {
					user.setPassword(passwordEncoder.encode(newPassword));
					user.setResetKey(null);
					user.setResetDate(null);
					userRepository.save(user);
					userCaches.evictEntryFromUserCaches(user.getEmail());
					return user;
				});
	}

	public Optional<User> requestPasswordReset(String mail) {
		return userRepository.findOneByEmailIgnoreCase(mail).filter(User::getActivated).map(user -> {
			user.setResetKey(RandomUtil.generateResetKey());
			user.setResetDate(Instant.now());
			userRepository.save(user);
			userCaches.evictEntryFromUserCaches(user.getEmail());
			return user;
		});
	}

	public User registerUser(UserDTO userDTO, String password) {
		userRepository.findOneByLogin(userDTO.getLogin().toLowerCase()).ifPresent(existingUser -> {
			boolean removed = removeNonActivatedUser(existingUser);
			if (!removed) {
				throw new LoginAlreadyUsedException();
			}
		});
		userRepository.findOneByEmailIgnoreCase(userDTO.getEmail().toLowerCase()).ifPresent(existingUser -> {
			boolean removed = removeNonActivatedUser(existingUser);
			if (!removed) {
				throw new EmailAlreadyUsedException();
			}
		});
		User newUser = new User();
		String encryptedPassword = passwordEncoder.encode(password);
		newUser.setLogin(userDTO.getLogin().toLowerCase());
		// new user gets initially a generated password
		newUser.setPassword(encryptedPassword);
		newUser.setFirstName(userDTO.getFirstName());
		newUser.setLastName(userDTO.getLastName());
		newUser.setEmail(userDTO.getEmail().toLowerCase());
		newUser.setImageUrl(userDTO.getImageUrl());
		newUser.setLangKey(userDTO.getLangKey());
		newUser.setActivated(false);
		// new user gets registration key
		newUser.setActivationKey(RandomUtil.generateActivationKey());
		newUser.setAuthorities(Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));
		userRepository.save(newUser);
		userCaches.evictEntryFromUserCaches(newUser.getEmail());
		LOG.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	private boolean removeNonActivatedUser(User existingUser) {
		if (existingUser.getActivated()) {
			return false;
		}
		userRepository.delete(existingUser);
		userCaches.evictEntryFromUserCaches(existingUser.getEmail());
		return true;
	}

	public User createUser(UserDTO userDTO) {
		User user = userDTO.toUser();
		user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
		user.setPassword("placeholder");
		user.setResetKey(RandomUtil.generateResetKey());
		user.setResetDate(Instant.now());
		userRepository.save(user);
		userCaches.evictEntryFromUserCaches(user.getEmail());
		LOG.debug("Created Information for User: {}", user);
		return user;
	}

	/**
	 * Update basic information (first name, last name, email, language) for the
	 * current user.
	 *
	 * @param firstName first name of user.
	 * @param lastName  last name of user.
	 * @param email     email id of user.
	 * @param langKey   language key.
	 * @param imageUrl  image URL of user.
	 */
	public void updateUser(String firstName, String lastName, String email, String langKey, String imageUrl) {
		SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).ifPresent(user -> {
			user.setFirstName(firstName);
			user.setLastName(lastName);
			user.setEmail(email.toLowerCase());
			user.setLangKey(langKey);
			user.setImageUrl(imageUrl);
			userRepository.save(user);
			userCaches.evictEntryFromUserCaches(user.getEmail());
			LOG.debug("Changed Information for User: {}", user);
		});
	}

	/**
	 * Update all information for a specific user, and return the modified user.
	 *
	 * @param userDTO user to update.
	 * @return updated user.
	 */
	public Optional<UserDTO> updateUser(UserDTO userDTO) {
		return Optional.of(userRepository.findById(userDTO.getId())).filter(Optional::isPresent).map(Optional::get)
				.map(user -> {
					userCaches.evictEntryFromUserCaches(user.getEmail());
					user.setLogin(userDTO.getLogin().toLowerCase());
					user.setFirstName(userDTO.getFirstName());
					user.setLastName(userDTO.getLastName());
					user.setEmail(userDTO.getEmail().toLowerCase());
					user.setImageUrl(userDTO.getImageUrl());
					user.setActivated(userDTO.isActivated());
					user.setLangKey(userDTO.getLangKey());
					if (userDTO.getAuthorities() != null) {
						user.setAuthorities(userDTO.getAuthorities().stream()
								.filter(s -> authorityRepository.findById(s).isPresent()).collect(Collectors.toSet()));
					}
					if (userDTO.getAssertionServicesEnabled()
							&& !user.getAuthorities().contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)) {
						user.getAuthorities().add(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
					} else if (!userDTO.getAssertionServicesEnabled()
							&& user.getAuthorities().contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)) {
						user.getAuthorities().remove(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
					}
					userRepository.save(user);
					userCaches.evictEntryFromUserCaches(user.getEmail());
					LOG.debug("Changed Information for User: {}", user);
					return user;
				}).map(UserDTO::valueOf);
	}

	public void deleteUser(String login) {
		userRepository.findOneByLogin(login).ifPresent(user -> {
			userRepository.delete(user);
			userCaches.evictEntryFromUserCaches(user.getEmail());
			LOG.debug("Deleted User: {}", user);
		});
	}

	public void clearUser(String id) {
		Optional<User> u = userRepository.findOneById(id);
		if (u.isPresent()) {
			LOG.debug("About to clear User with id: {}", id);
			User user = u.get();
			String email = user.getEmail();
			userCaches.evictEntryFromUserCaches(email);

			user.setActivated(false);
			user.setActivationKey(null);
			user.setAuthorities(new HashSet<String>());
			user.setEmail(id + "@deleted.orcid.org");
			user.setFirstName(null);
			user.setImageUrl(null);
			user.setLangKey(null);
			user.setLastName(null);
			user.setLogin(id);
			user.setPassword(RandomStringUtils.randomAlphanumeric(60));
			user.setResetDate(null);
			user.setResetKey(null);
			user.setDeleted(Boolean.TRUE);
			user.setLastModifiedDate(Instant.now());
			userRepository.save(user);
			LOG.debug("User cleared: {}", id);
		}
	}

	public void changePassword(String currentClearTextPassword, String newPassword) {
		SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin).ifPresent(user -> {
			String currentEncryptedPassword = user.getPassword();
			if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
				throw new InvalidPasswordException();
			}
			String encryptedPassword = passwordEncoder.encode(newPassword);
			user.setPassword(encryptedPassword);
			userRepository.save(user);
			userCaches.evictEntryFromUserCaches(user.getEmail());
			LOG.debug("Changed password for User: {}", user);
		});
	}

	public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
		return userRepository.findByDeletedFalse(pageable).map(UserDTO::valueOf);
	}

	public Optional<User> getUserWithAuthoritiesByLogin(String login) {
		return userRepository.findOneByLogin(login);
	}

	public Optional<User> getUserWithAuthorities(String id) {
		return userRepository.findById(id);
	}

	public Optional<User> getUserWithAuthorities() {
		return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByLogin);
	}

	public List<User> findAllByLoginOrEmail(String login, String email) {
		return userRepository.findAllByLoginOrEmail(login, email);
	}

	public void removeAuthorityFromUser(String id, String authority) {
		Optional<User> existing = getUserWithAuthorities(id);
		if (!existing.isPresent()) {
			throw new BadRequestAlertException("User not present", "user", null);
		}

		User user = existing.get();
		if (user.getAuthorities() != null && !user.getAuthorities().isEmpty()) {
			user.setAuthorities(
					user.getAuthorities().stream().filter(a -> !a.equals(authority)).collect(Collectors.toSet()));
		}
		userRepository.save(user);
	}

	/**
	 * Not activated users should be automatically deleted after 3 days.
	 * <p>
	 * This is scheduled to get fired everyday, at 01:00 (am).
	 */
	@Scheduled(cron = "0 0 1 * * ?")
	public void removeNotActivatedUsers() {
		userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
				Instant.now().minus(3, ChronoUnit.DAYS)).forEach(user -> {
					LOG.debug("Deleting not activated user {}", user.getLogin());
					userRepository.delete(user);
					userCaches.evictEntryFromUserCaches(user.getEmail());
				});
	}

	/**
	 * Gets a list of all the authorities.
	 * 
	 * @return a list of all the authorities.
	 */
	public List<String> getAuthorities() {
		return authorityRepository.findAll().stream().map(Authority::getName).collect(Collectors.toList());
	}

	public UsersUpload uploadUserCSV(InputStream inputStream, String createdBy) {
		UsersUpload usersUpload = null;
		try {
			usersUpload = usersUploadReader.readUsersUpload(inputStream, createdBy);
		} catch (IOException e) {
			LOG.warn("Error reading user upload", e);
			throw new RuntimeException(e);
		}

		usersUpload.getUserDTOs().forEach(userDTO -> {
			String salesforceId = userDTO.getSalesforceId();
			if (!memberSettingsExists(salesforceId)) {
				String errorMessage = String.format("Member not found with salesforceId %s", salesforceId);
				throw new MemberNotFoundException(errorMessage);
			}

			Optional<User> existing = getUserWithAuthoritiesByLogin(userDTO.getLogin());
			if (existing.isPresent()) {
				updateUser(userDTO);
			} else {
				createUser(userDTO);
			}
		});
		return usersUpload;
	}

	private Boolean memberSettingsExists(String salesforceId) {
		Optional<Member> existingMemberSettings = memberRepository.findBySalesforceId(salesforceId);
		return existingMemberSettings.isPresent();
	}

}
