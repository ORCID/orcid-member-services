package org.orcid.user.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.orcid.user.config.Constants;
import org.orcid.user.domain.Authority;
import org.orcid.user.domain.User;
import org.orcid.user.repository.AuthorityRepository;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.security.AuthoritiesConstants;
import org.orcid.user.security.SecurityUtils;
import org.orcid.user.service.cache.UserCaches;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.service.mapper.UserMapper;
import org.orcid.user.service.util.RandomUtil;
import org.orcid.user.upload.UserUpload;
import org.orcid.user.upload.UserUploadReader;
import org.orcid.user.web.rest.errors.AccountResourceException;
import org.orcid.user.web.rest.errors.BadRequestAlertException;
import org.orcid.user.web.rest.errors.EmailAlreadyUsedException;
import org.orcid.user.web.rest.errors.EmailNotFoundException;
import org.orcid.user.web.rest.errors.ExpiredKeyException;
import org.orcid.user.web.rest.errors.InvalidKeyException;
import org.orcid.user.web.rest.errors.InvalidPasswordException;
import org.orcid.user.web.rest.errors.LoginAlreadyUsedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    public static final int RESET_KEY_LIFESPAN_IN_SECONDS = 86400;

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

    @Autowired
    private UserMapper userMapper;

    public void completePasswordReset(String newPassword, String key) throws ExpiredKeyException, InvalidKeyException {
        LOG.debug("Reset user password for reset key {}", key);
        if (!validResetKey(key)) {
            throw new InvalidKeyException();
        }

        if (expiredResetKey(key)) {
            throw new ExpiredKeyException();
        }

        User user = userRepository.findOneByResetKey(key).get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetKey(null);
        user.setResetDate(null);
        user.setActivated(true);
        userRepository.save(user);
        userCaches.evictEntryFromEmailCache(user.getEmail());
    }

    public boolean validResetKey(String key) {
        Optional<User> resetUser = userRepository.findOneByResetKey(key);
        return resetUser.isPresent();
    }

    public boolean expiredResetKey(String key) {
        Optional<User> resetUser = userRepository.findOneByResetKey(key);
        return resetUser.isPresent() && !resetUser.get().getResetDate().isAfter(Instant.now().minusSeconds(RESET_KEY_LIFESPAN_IN_SECONDS));
    }

    public void resendActivationEmail(String previousKey) {
        User user = userRepository.findOneByResetKey(previousKey).get();
        sendActivationEmail(user);
    }

    public Optional<User> requestPasswordReset(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail).filter(User::getActivated).map(user -> {
            user.setResetKey(RandomUtil.generateResetKey());
            user.setResetDate(Instant.now());
            userRepository.save(user);
            userCaches.evictEntryFromEmailCache(user.getEmail());
            return user;
        });
    }

    public User registerUser(UserDTO userDTO, String password) {
        userRepository.findOneByEmailIgnoreCase(userDTO.getEmail().toLowerCase()).ifPresent(existingUser -> {
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
        newUser.setAuthorities(getAuthoritiesForUser(userDTO, userDTO.getIsAdmin()));
        userRepository.save(newUser);
        userCaches.evictEntryFromEmailCache(newUser.getEmail());
        LOG.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.getActivated()) {
            return false;
        }
        userRepository.delete(existingUser);
        userCaches.evictEntryFromEmailCache(existingUser.getEmail());
        return true;
    }

    public User createUser(UserDTO userDTO) {
        userDTO.setAuthorities(getAuthoritiesForUser(userDTO, userDTO.getIsAdmin()));

        User user = userMapper.toUser(userDTO);
        user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        user.setPassword("placeholder");
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        userRepository.save(user);
        userCaches.evictEntryFromEmailCache(user.getEmail());
        LOG.debug("Created User: {}", user);

        LOG.debug("Sending email to user {}", user.getEmail());
        mailService.sendCreationEmail(user);

        return user;
    }

    /**
     * Update basic information (first name, last name, email, language) for the
     * current user.
     *
     * @param firstName
     *            first name of user.
     * @param lastName
     *            last name of user.
     * @param email
     *            email id of user.
     * @param langKey
     *            language key.
     * @param imageUrl
     *            image URL of user.
     */
    public void updateAccount(String firstName, String lastName, String email, String langKey, String imageUrl) {
        User user = getCurrentUser();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLangKey(langKey);
        user.setImageUrl(imageUrl);
        userRepository.save(user);
        userCaches.evictEntryFromEmailCache(user.getEmail());
        LOG.debug("Changed Information for User: {}", user);
    }

    /**
     * Update all information for a specific user, and return the modified user.
     *
     * @param userDTO
     *            user to update.
     * @return updated user.
     */
    public Optional<UserDTO> updateUser(UserDTO userDTO) {
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail());
        if (!existingUser.isPresent()) {
            throw new EmailNotFoundException();
        }

        checkUpdateConstraints(existingUser, userDTO);
        User user = existingUser.get();
        boolean previouslyOwner = user.getMainContact() != null ? user.getMainContact() : false;
        boolean owner = userDTO.getMainContact() != null ? userDTO.getMainContact() : false;

        if (owner && !previouslyOwner) {
            List<User> owners = userRepository.findAllByMainContactIsTrueAndDeletedIsFalseAndSalesforceId(userDTO.getSalesforceId());
            for (User prevOwner : owners) {
                if (!StringUtils.equals(prevOwner.getId(), userDTO.getId())) {
                    removeOwnershipFromUser(prevOwner.getEmail());
                }
            }
            userDTO.getAuthorities().add(AuthoritiesConstants.ORG_OWNER);

        }

        userCaches.evictEntryFromEmailCache(user.getEmail());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setImageUrl(userDTO.getImageUrl());
        user.setMainContact(userDTO.getMainContact());
        user.setSalesforceId(userDTO.getSalesforceId());
        user.setMemberName(memberService.getMemberNameBySalesforce(userDTO.getSalesforceId()));
        user.setLoginAs(userDTO.getLoginAs());
        user.setLangKey(userDTO.getLangKey() != null ? userDTO.getLangKey() : user.getLangKey());
        user.setAuthorities(getAuthoritiesForUser(userDTO, userDTO.getIsAdmin()));

        if (!StringUtils.equals(user.getEmail(), userDTO.getEmail().toLowerCase())) {
            user.setEmail(userDTO.getEmail().toLowerCase());
            user.setActivated(false);
            user.setActivationKey(RandomUtil.generateResetKey());
            user.setActivationDate(Instant.now());
            mailService.sendActivationEmail(user);
        }

        if (user.getSalesforceId() != null && userDTO.getSalesforceId() != null && !user.getSalesforceId().equals(userDTO.getSalesforceId())) {
            user.setSalesforceId(userDTO.getSalesforceId());
            user.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
            user.setLastModifiedDate(Instant.now());
        }

        userRepository.save(user);
        userCaches.evictEntryFromEmailCache(user.getEmail());

        if (owner && !previouslyOwner) {
            String member = memberService.getMemberNameBySalesforce(user.getSalesforceId());
            mailService.sendOrganizationOwnerChangedMail(user, member);
        }

        return Optional.of(userMapper.toUserDTO(user));
    }

    private void checkUpdateConstraints(Optional<User> existingUser, UserDTO userDTO) {
        if (existingUser.isPresent() && (!existingUser.get().getId().equals(userDTO.getId()))) {
            throw new EmailAlreadyUsedException();
        }
        existingUser = userRepository.findOneByEmailIgnoreCase(userDTO.getEmail().toLowerCase());
        if (existingUser.isPresent() && (!existingUser.get().getId().equals(userDTO.getId()))) {
            throw new LoginAlreadyUsedException();
        }
    }

    public void deleteUser(String login) {
        userRepository.findOneByEmailIgnoreCase(login).ifPresent(user -> {
            userRepository.delete(user);
            userCaches.evictEntryFromEmailCache(user.getEmail());
            LOG.debug("Deleted User: {}", user);
        });
    }

    public void clearUser(String id) {
        Optional<User> u = userRepository.findOneById(id);
        if (u.isPresent()) {
            LOG.debug("About to clear User with id: {}", id);
            User user = u.get();
            String email = user.getEmail();
            userCaches.evictEntryFromEmailCache(email);

            user.setActivated(false);
            user.setActivationKey(null);
            user.setAuthorities(new HashSet<String>());
            user.setEmail(id + "@deleted.orcid.org");
            user.setFirstName(null);
            user.setImageUrl(null);
            user.setLangKey(null);
            user.setLastName(null);
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
        SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByEmailIgnoreCase).ifPresent(user -> {
            String currentEncryptedPassword = user.getPassword();
            if (!passwordEncoder.matches(currentClearTextPassword, currentEncryptedPassword)) {
                throw new InvalidPasswordException();
            }
            String encryptedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encryptedPassword);
            userRepository.save(user);
            userCaches.evictEntryFromEmailCache(user.getEmail());
            LOG.debug("Changed password for User: {}", user);
        });
    }

    public Optional<User> sendActivationEmail(String mail) {
        return userRepository.findOneByEmailIgnoreCase(mail).map(user -> {
            sendActivationEmail(user);
            return user;
        });
    }

    public Page<UserDTO> getAllManagedUsers(Pageable pageable) {
        return userRepository.findByDeletedFalse(pageable).map(u -> userMapper.toUserDTO(u));
    }

    public Page<UserDTO> getAllManagedUsers(Pageable pageable, String filter) {
        return userRepository
                .findByDeletedIsFalseAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndEmailContainingIgnoreCase(
                        filter, filter, filter, filter, pageable)
                .map(u -> userMapper.toUserDTO(u));
    }

    public Optional<User> getUserWithAuthoritiesByLogin(String login) {
        return userRepository.findOneByEmailIgnoreCase(login);
    }

    public Optional<User> getUserWithAuthorities(String id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserWithAuthorities() {
        return SecurityUtils.getCurrentUserLogin().flatMap(userRepository::findOneByEmailIgnoreCase);
    }

    public List<User> findAllByEmail(String login, String email) {
        return userRepository.findAllByEmailIgnoreCase(email);
    }

    public void removeAuthorityFromUser(String id, String authority) {
        Optional<User> existing = getUserWithAuthorities(id);
        if (!existing.isPresent()) {
            throw new BadRequestAlertException("User not present " + id, "user", null);
        }

        User user = existing.get();
        if (user.getAuthorities() != null && !user.getAuthorities().isEmpty()) {
            user.setAuthorities(user.getAuthorities().stream().filter(a -> !a.equals(authority)).collect(Collectors.toSet()));
        }
        userRepository.save(user);
    }

    public void removeOwnershipFromUser(String id) {
        Optional<User> existing = getUserWithAuthoritiesByLogin(id);
        if (!existing.isPresent()) {
            throw new BadRequestAlertException("User not present " + id, "user", null);
        }

        User user = existing.get();
        if (user.getAuthorities() != null && !user.getAuthorities().isEmpty()) {
            user.setAuthorities(user.getAuthorities().stream().filter(a -> !a.equals(AuthoritiesConstants.ORG_OWNER)).collect(Collectors.toSet()));
        }
        user.setMainContact(false);
        userRepository.save(user);
    }

    /**
     * Not activated users should be automatically deleted after 3 days.
     * <p>
     * This is scheduled to get fired everyday, at 01:00 (am).
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void removeNotActivatedUsers() {
        userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant.now().minus(3, ChronoUnit.DAYS)).forEach(user -> {
            LOG.debug("Deleting not activated user {}", user.getEmail());
            userRepository.delete(user);
            userCaches.evictEntryFromEmailCache(user.getEmail());
        });
    }

    /**
     * Task to populate empty member names, to update 10 every five minutes
     */
    @Scheduled(initialDelay = 300000l, fixedDelay = 300000l)
    public void updateMemberNames() {
        Page<User> page = userRepository.findByMemberName(PageRequest.of(0, 10), null);
        page.getContent().stream().filter(u -> !StringUtils.isBlank(u.getSalesforceId())).forEach(u -> {
            LOG.info("Populating member name field for user {}", u.getEmail());
            u.setMemberName(memberService.getMemberNameBySalesforce(u.getSalesforceId()));
            userRepository.save(u);
            userCaches.evictEntryFromEmailCache(u.getEmail());
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

    public UserUpload uploadUserCSV(InputStream inputStream, User currentUser) {
        UserUpload usersUpload = null;
        try {
            usersUpload = usersUploadReader.readUsersUpload(inputStream, currentUser);
        } catch (IOException e) {
            LOG.warn("Error reading user upload", e);
            throw new RuntimeException(e);
        }

        usersUpload.getUserDTOs().forEach(userDTO -> {
            String salesforceId = userDTO.getSalesforceId();
            Optional<User> existing = getUserWithAuthoritiesByLogin(userDTO.getEmail());
            if (!existing.isPresent() && !userRepository.findOneBySalesforceIdAndMainContactIsTrue(salesforceId).isPresent()) {
                userDTO.setMainContact(true);
                createUser(userDTO);
            }
        });
        return usersUpload;
    }

    public Boolean memberExists(String salesforceId) {
        return memberService.memberExistsWithSalesforceId(salesforceId);
    }

    public Boolean memberSuperadminEnabled(String salesforceId) {
        return memberService.memberExistsWithSalesforceIdAndSuperadminEnabled(salesforceId);
    }

    public List<UserDTO> getAllUsersBySalesforceId(String salesforceId) {
        List<User> users = userRepository.findBySalesforceIdAndDeletedIsFalse(salesforceId);
        return users.stream().map(u -> userMapper.toUserDTO(u)).collect(Collectors.toList());
    }

    public Page<UserDTO> getAllUsersBySalesforceId(Pageable pageable, String salesforceId) {
        return userRepository.findBySalesforceIdAndDeletedIsFalse(pageable, salesforceId).map(u -> userMapper.toUserDTO(u));
    }

    public Page<UserDTO> getAllUsersBySalesforceId(Pageable pageable, String salesforceId, String filter) {
        return userRepository
                .findByDeletedIsFalseAndSalesforceIdAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndSalesforceIdAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndSalesforceIdAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndSalesforceIdAndEmailContainingIgnoreCase(
                        pageable, salesforceId, filter, salesforceId, filter, salesforceId, filter, salesforceId, filter)
                .map(u -> userMapper.toUserDTO(u));
    }

    private void sendActivationEmail(User user) {
        user.setActivated(false);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        userRepository.save(user);
        userCaches.evictEntryFromEmailCache(user.getEmail());
        mailService.sendActivationEmail(user);
    }

    private Set<String> getAuthoritiesForUser(UserDTO userDTO, boolean isAdmin) {
        Set<String> authorities = Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet());
        if (!org.apache.commons.lang3.StringUtils.isBlank(userDTO.getSalesforceId())) {
            if (memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(userDTO.getSalesforceId())) {
                authorities.add(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
            }
            if (memberService.memberIsConsortiumLead(userDTO.getSalesforceId())) {
                authorities.add(AuthoritiesConstants.CONSORTIUM_LEAD);
            }
        }

        if (userDTO.getMainContact() != null) {
            if (userDTO.getMainContact()) {
                authorities.add(AuthoritiesConstants.ORG_OWNER);
            }
        }
        if (isAdmin) {
            authorities.add(AuthoritiesConstants.ADMIN);
        }
        return authorities;
    }

    public boolean hasOwnerForSalesforceId(String salesforceId) {
        List<User> owners = userRepository.findAllByMainContactIsTrueAndDeletedIsFalseAndSalesforceId(salesforceId);
        if (owners.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Returns the user currently logged in or being impersonated.
     * 
     * @return
     */
    public User getCurrentUser() {
        String login = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new AccountResourceException("Current user login not found"));
        Optional<User> user = userRepository.findOneByEmailIgnoreCase(login);

        if (StringUtils.isEmpty(user.get().getLoginAs())) {
            return user.get();
        }

        return userRepository.findOneByEmailIgnoreCase(user.get().getLoginAs()).get();
    }

}
