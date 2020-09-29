package org.orcid.user.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.orcid.user.config.Constants;
import org.orcid.user.domain.Authority;
import org.orcid.user.domain.User;
import org.orcid.user.repository.AuthorityRepository;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.security.AuthoritiesConstants;
import org.orcid.user.security.SecurityUtils;
import org.orcid.user.service.cache.UserCaches;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.service.util.RandomUtil;
import org.orcid.user.upload.UserUpload;
import org.orcid.user.upload.UserUploadReader;
import org.orcid.user.web.rest.errors.BadRequestAlertException;
import org.orcid.user.web.rest.errors.EmailAlreadyUsedException;
import org.orcid.user.web.rest.errors.InvalidPasswordException;
import org.orcid.user.web.rest.errors.LoginAlreadyUsedException;
import org.orcid.user.web.rest.errors.MemberNotFoundException;
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
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthorityRepository authorityRepository;

	@Autowired
	private UserCaches userCaches;

	@Autowired
	private UserUploadReader usersUploadReader;

	@Autowired
	private MemberService memberService;

	@Autowired
	private MailService mailService;

	public Optional<User> completePasswordReset(String newPassword, String key) {
		LOG.debug("Reset user password for reset key {}", key);
		return userRepository.findOneByResetKey(key)
				.filter(user -> user.getResetDate().isAfter(Instant.now().minusSeconds(86400))).map(user -> {
					user.setPassword(passwordEncoder.encode(newPassword));
					user.setResetKey(null);
					user.setResetDate(null);
					user.setActivated(true);
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
		newUser.setAuthorities(getAuthoritiesForUser(userDTO.getSalesforceId(), userDTO.getIsAdmin()));
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
		userDTO.setAuthorities(getAuthoritiesForUser(userDTO.getSalesforceId(),userDTO.getIsAdmin()));

		User user = userDTO.toUser();
		user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
		user.setPassword("placeholder");
		user.setResetKey(RandomUtil.generateResetKey());
		user.setResetDate(Instant.now());
		userRepository.save(user);
		userCaches.evictEntryFromUserCaches(user.getEmail());
		LOG.debug("Created User: {}", user);

		LOG.debug("Sending email to user {}", user.getEmail());
		mailService.sendCreationEmail(user);

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
    		        if(!StringUtils.equals(user.getEmail(),email.toLowerCase()) ){
    		            user.setEmail(email.toLowerCase());
                            user.setActivated(false);
                            user.setActivationKey(RandomUtil.generateResetKey());
                            user.setActivationDate(Instant.now());
                            mailService.sendActivationEmail(user);
                        }
			user.setFirstName(firstName);
			user.setLastName(lastName);
			user.setLangKey(langKey);
			user.setImageUrl(imageUrl);
			user.setAuthorities(getAuthoritiesForUser(user.getSalesforceId(),false));
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
					user.setImageUrl(userDTO.getImageUrl());
					user.setMainContact(userDTO.getMainContact());
					user.setSalesforceId(userDTO.getSalesforceId());
					//user.setActivated(userDTO.isActivated());
                    if (userDTO.getLangKey() != null) {
                        user.setLangKey(userDTO.getLangKey());
                    }
					user.setAuthorities(getAuthoritiesForUser(userDTO.getSalesforceId(), userDTO.getIsAdmin()));
					if(!StringUtils.equals(user.getEmail(),userDTO.getEmail().toLowerCase()) ){
					    user.setEmail(userDTO.getEmail().toLowerCase());
					    user.setActivated(false);
                                            user.setActivationKey(RandomUtil.generateResetKey());
                                            user.setActivationDate(Instant.now());
                                            mailService.sendActivationEmail(user);
                                        }
                    if (user.getSalesforceId() != null && userDTO.getSalesforceId() != null &&
                        !user.getSalesforceId().equals(userDTO.getSalesforceId())) {
                        user.setSalesforceId(userDTO.getSalesforceId());
                        user.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
                        user.setLastModifiedDate(Instant.now());
                    }

					userRepository.save(user);
					userCaches.evictEntryFromUserCaches(user.getEmail());
					LOG.debug("Changed Information for User: {}", user);
					return user;
				}).map(UserDTO::valueOf);
	}

//    public Optional<UserDTO> updateUserSalesForceId(UserDTO userDTO, String newSalesforceId) {
//        return Optional.of(userRepository.findById(userDTO.getId())).filter(Optional::isPresent).map(Optional::get)
//            .map(user -> {
//                user.setSalesforceId(newSalesforceId);
//                user.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
//                user.setLastModifiedDate(Instant.now());
//                userRepository.save(user);
//                return user;
//            }).map(UserDTO::valueOf);
//    }

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

        public Optional<User> sendActivationEmail(String mail) {
            return userRepository.findOneByEmailIgnoreCase(mail).map(user -> {
                user.setActivated(false);
                user.setResetKey(RandomUtil.generateResetKey());
                user.setResetDate(Instant.now());
                userRepository.save(user);
                userCaches.evictEntryFromUserCaches(user.getEmail());
                mailService.sendActivationEmail(user);
                return user;
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
			throw new BadRequestAlertException("User not present " + id, "user", null);
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

	public UserUpload uploadUserCSV(InputStream inputStream, String createdBy) {
		UserUpload usersUpload = null;
		try {
			usersUpload = usersUploadReader.readUsersUpload(inputStream, createdBy);
		} catch (IOException e) {
			LOG.warn("Error reading user upload", e);
			throw new RuntimeException(e);
		}

		usersUpload.getUserDTOs().forEach(userDTO -> {
			String salesforceId = userDTO.getSalesforceId();

			if (!memberExists(salesforceId)) {
				String errorMessage = String.format("Member not found with salesforceId %s", salesforceId);
                Map<String, String> params = new HashMap<>();
                params.put("params", salesforceId);
				throw new MemberNotFoundException(errorMessage, params);
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

	public Boolean memberExists(String salesforceId) {
		return memberService.memberExistsWithSalesforceId(salesforceId);
	}

	public List<UserDTO> getUsersBySalesforceId(String salesforceId) {
		List<User> users = userRepository.findBySalesforceId(salesforceId);
		return users.stream().map(UserDTO::valueOf).collect(Collectors.toList());
	}

	private Set<String> getAuthoritiesForUser(String salesforceId, Boolean isAdmin) {
		Set<String> authorities = Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet());
		if (memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(salesforceId)) {
			authorities.add(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
		}
		if(isAdmin) {
		    authorities.add(AuthoritiesConstants.ADMIN);
		}
		return authorities;
	}

}
