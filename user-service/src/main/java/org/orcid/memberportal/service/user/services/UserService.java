package org.orcid.memberportal.service.user.services;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.orcid.memberportal.service.user.config.Constants;
import org.orcid.memberportal.service.user.domain.ActivationReminder;
import org.orcid.memberportal.service.user.domain.Authority;
import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.dto.UserDTO;
import org.orcid.memberportal.service.user.mapper.UserMapper;
import org.orcid.memberportal.service.user.repository.AuthorityRepository;
import org.orcid.memberportal.service.user.repository.UserRepository;
import org.orcid.memberportal.service.user.security.AuthoritiesConstants;
import org.orcid.memberportal.service.user.security.EncryptUtil;
import org.orcid.memberportal.service.user.security.MfaAuthenticationFailureException;
import org.orcid.memberportal.service.user.security.MfaSetup;
import org.orcid.memberportal.service.user.security.SecurityUtils;
import org.orcid.memberportal.service.user.upload.UserUpload;
import org.orcid.memberportal.service.user.upload.UserUploadReader;
import org.orcid.memberportal.service.user.util.RandomUtil;
import org.orcid.memberportal.service.user.web.rest.errors.AccountResourceException;
import org.orcid.memberportal.service.user.web.rest.errors.BadRequestAlertException;
import org.orcid.memberportal.service.user.web.rest.errors.EmailAlreadyUsedException;
import org.orcid.memberportal.service.user.web.rest.errors.EmailNotFoundException;
import org.orcid.memberportal.service.user.web.rest.errors.ExpiredKeyException;
import org.orcid.memberportal.service.user.web.rest.errors.InvalidKeyException;
import org.orcid.memberportal.service.user.web.rest.errors.InvalidPasswordException;
import org.orcid.memberportal.service.user.web.rest.errors.LoginAlreadyUsedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import net.glxn.qrgen.QRCode;

/**
 * Service class for managing users.
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    static final int BACKUP_CODE_LENGTH = 10;

    static final int BACKUP_CODE_BATCH_SIZE = 14;

    public static final int RESET_KEY_LIFESPAN_IN_SECONDS = 86400;

    private static final String MFA_QR_CODE_ACCOUNT_NAME = "ORCID Member Portal";

    /**
     * ordered list for days activation email is resent to users who haven't
     * activated yet
     */
    private static final int[] ACTIVATION_REMINDER_DAYS = new int[] { 7, 30 };

    public static final int BATCH_SIZE = 100;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserUploadReader usersUploadReader;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EncryptUtil encryptUtil;

    public boolean updateUsersSalesforceId(String from, String to) {
        return updateUsersSalesforceId(from, to, true);
    }

    private boolean updateUsersSalesforceId(String from, String to, boolean rollback) {
        try {
            Pageable pageable = PageRequest.of(0, BATCH_SIZE, new Sort(Direction.ASC, "created"));
            Page<User> page = userRepository.findBySalesforceIdAndDeletedIsFalse(pageable, from);
            while (!page.isEmpty()) {
                page.forEach(u -> {
                    u.setSalesforceId(to);
                    u.setLastModifiedDate(Instant.now());
                    userRepository.save(u);
                });

                // repeat until no more left in db with old sf id
                page = userRepository.findBySalesforceIdAndDeletedIsFalse(pageable, from);
            }
        } catch (Exception e) {
            LOG.error("Error bulk updating users from salesforce '" + from + "' to salesforce '" + to + "'", e);
            if (rollback) {
                LOG.info("Attempting to RESET user salesforce ids from '{}' to '{}'", new Object[] { to, from });
                boolean success = updateUsersSalesforceId(to, from, false);
                if (success) {
                    LOG.info("Succeeded in RESETTING user salesforce ids from '{}' to '{}'", new Object[] { to, from });
                    return false;
                } else {
                    LOG.error("Failed to reset users from '{}' to '{}'", new Object[] { to, from });
                    LOG.error("Operation to update users salesforce ids from '{}' to '{}' has failed but there may be users with new sf id of '{}' in the database!",
                            new Object[] { from, to, to });
                    return false;
                }
            }
        }
        return true;
    }

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
        user.setActivationDate(Instant.now());
        userRepository.save(user);
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
        newUser.setAuthorities(
                getAuthoritiesForUser(userDTO.getSalesforceId(), userDTO.getMainContact() != null && userDTO.getMainContact().booleanValue(), userDTO.getIsAdmin()));
        userRepository.save(newUser);
        LOG.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    private boolean removeNonActivatedUser(User existingUser) {
        if (existingUser.getActivated()) {
            return false;
        }
        userRepository.delete(existingUser);
        return true;
    }

    public User createUser(UserDTO userDTO) {
        userDTO.setAuthorities(
                getAuthoritiesForUser(userDTO.getSalesforceId(), userDTO.getMainContact() != null && userDTO.getMainContact().booleanValue(), userDTO.getIsAdmin()));

        User user = userMapper.toUser(userDTO);
        user.setLangKey(Constants.DEFAULT_LANGUAGE); // default language
        user.setPassword("placeholder");
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        user.setActivated(false);
        userRepository.save(user);

        mailService.sendActivationEmail(user);
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
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setImageUrl(userDTO.getImageUrl());
        user.setMainContact(userDTO.getMainContact());
        user.setSalesforceId(userDTO.getSalesforceId());
        user.setMemberName(memberService.getMemberNameBySalesforce(userDTO.getSalesforceId()));
        user.setLoginAs(userDTO.getLoginAs());
        user.setLangKey(userDTO.getLangKey() != null ? userDTO.getLangKey() : user.getLangKey());
        user.setAuthorities(
                getAuthoritiesForUser(userDTO.getSalesforceId(), userDTO.getMainContact() != null && userDTO.getMainContact().booleanValue(), userDTO.getIsAdmin()));

        if (user.getSalesforceId() != null && userDTO.getSalesforceId() != null && !user.getSalesforceId().equals(userDTO.getSalesforceId())) {
            user.setSalesforceId(userDTO.getSalesforceId());
            user.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
            user.setLastModifiedDate(Instant.now());
        }
        userRepository.save(user);

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
            LOG.debug("Deleted User: {}", user);
        });
    }

    public void clearUser(String id) {
        Optional<User> u = userRepository.findOneById(id);
        if (u.isPresent()) {
            LOG.debug("About to clear User with id: {}", id);
            User user = u.get();
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

    public void sendActivationReminders() {
        List<User> users = userRepository.findAllByActivatedIsFalseAndDeletedIsFalse();
        users.forEach(u -> {
            List<ActivationReminder> remindersSent = u.getActivationReminders();
            for (int daysElapsed : ACTIVATION_REMINDER_DAYS) {
                LocalDateTime userCreated = LocalDateTime.ofInstant(u.getCreatedDate(), ZoneId.systemDefault());
                if (reminderDue(userCreated, daysElapsed)) {
                    if (!reminderSentForNumDaysElapsed(remindersSent, daysElapsed)) {
                        sendActivationEmail(u);
                        logReminderSent(daysElapsed, u);
                    }
                }
            }
        });
    }

    private void logReminderSent(int daysElapsed, User u) {
        if (u.getActivationReminders() == null) {
            u.setActivationReminders(new ArrayList<>());
        }
        u.getActivationReminders().add(new ActivationReminder(daysElapsed, Instant.now()));
        userRepository.save(u);
    }

    private boolean reminderDue(LocalDateTime userCreated, int daysElapsed) {
        return userCreated.until(LocalDateTime.now(), ChronoUnit.DAYS) >= daysElapsed;
    }

    private boolean reminderSentForNumDaysElapsed(List<ActivationReminder> remindersSent, int daysElapsed) {
        if (remindersSent != null) {
            for (ActivationReminder reminder : remindersSent) {
                if (reminder.getDaysElapsed() == daysElapsed) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendActivationEmail(User user) {
        user.setActivated(false);
        user.setResetKey(RandomUtil.generateResetKey());
        user.setResetDate(Instant.now());
        userRepository.save(user);
        mailService.sendActivationEmail(user);
    }

    private Set<String> getAuthoritiesForUser(String salesforceId, boolean mainContact, boolean isAdmin) {
        Set<String> authorities = Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet());
        if (!org.apache.commons.lang3.StringUtils.isBlank(salesforceId)) {
            if (memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(salesforceId)) {
                authorities.add(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED);
            }

            if (memberService.memberIsConsortiumLead(salesforceId)) {
                authorities.add(AuthoritiesConstants.CONSORTIUM_LEAD);
            }
        }

        if (mainContact) {
            authorities.add(AuthoritiesConstants.ORG_OWNER);
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

    public MfaSetup getMfaSetup() {
        String secret = Base32.random();
        String qrCode = String.format("otpauth://totp/%s?secret=%s&issuer=%s", SecurityUtils.getCurrentUserLogin().get(), secret, MFA_QR_CODE_ACCOUNT_NAME);
        MfaSetup mfaSetup = new MfaSetup();
        mfaSetup.setSecret(secret);
        mfaSetup.setQrCode(QRCode.from(qrCode).withSize(250, 250).stream().toByteArray());
        return mfaSetup;
    }

    public List<String> enableMfa(MfaSetup mfaSetup) {
        validateOtp(mfaSetup.getOtp(), mfaSetup.getSecret());

        List<String> backupCodes = generateBackupCodes();
        List<String> hashedBackupCodes = hashBackupCodes(backupCodes);

        User user = getCurrentUser();
        user.setMfaEnabled(true);
        user.setMfaEncryptedSecret(encryptUtil.encrypt(mfaSetup.getSecret()));
        user.setMfaBackupCodes(hashedBackupCodes);
        userRepository.save(user);

        return backupCodes;
    }

    public boolean validMfaCode(String username, String code) {
        Optional<User> user = userRepository.findOneByEmailIgnoreCase(username);
        String encryptedSecret = user.get().getMfaEncryptedSecret();
        String decryptedSecret = encryptUtil.decrypt(encryptedSecret);

        try {
            validateOtp(code, decryptedSecret);
            return true;
        } catch (MfaAuthenticationFailureException e) {
            return validMfaBackupCode(user.get(), code);
        }
    }

    private boolean validMfaBackupCode(User user, String code) {
        boolean validBackupCode = false;
        List<String> filteredBackupCodes = user.getMfaBackupCodes().stream().filter(c -> !passwordEncoder.matches(code, c)).collect(Collectors.toList());
        if (filteredBackupCodes.size() == user.getMfaBackupCodes().size() - 1) {
            // match found and removed
            validBackupCode = true;
            user.setMfaBackupCodes(filteredBackupCodes);
            userRepository.save(user);
        }
        return validBackupCode;
    }

    public void validateOtp(String otp, String secret) {
        otp = otp.replaceAll("\\s", "");
        if (!validLong(otp)) {
            throw new MfaAuthenticationFailureException("Invalid OTP");
        }

        Totp totp = new Totp(secret);
        if (!totp.verify(otp)) {
            throw new MfaAuthenticationFailureException("Invalid OTP");
        }
    }

    public void disableMfa() {
        User user = getCurrentUser();
        user.setMfaEnabled(false);
        user.setMfaEncryptedSecret(null);
        user.setMfaBackupCodes(null);
        userRepository.save(user);
    }

    public boolean isMfaEnabled(String username) {
        Optional<User> user = userRepository.findOneByEmailIgnoreCase(username);
        if (!user.isPresent()) {
            return false;
        } else {
            Boolean mfaEnabled = user.get().getMfaEnabled();
            return mfaEnabled != null ? mfaEnabled.booleanValue() : false;
        }
    }

    public boolean refreshAuthorities(String salesforceId) {
        LOG.info("Refreshing user authorities for salesforce id {}", salesforceId);
        try {
            Pageable pageable = PageRequest.of(0, BATCH_SIZE, new Sort(Direction.ASC, "created"));
            Page<User> page = userRepository.findBySalesforceIdAndDeletedIsFalse(pageable, salesforceId);
            while (!page.isEmpty()) {
                page.forEach(u -> {
                    u.setAuthorities(getAuthoritiesForUser(u.getSalesforceId(), u.getMainContact() != null && u.getMainContact().booleanValue(),
                            u.getAuthorities().contains(AuthoritiesConstants.ADMIN)));
                    u.setLastModifiedDate(Instant.now());
                    userRepository.save(u);
                });

                page = userRepository.findBySalesforceIdAndDeletedIsFalse(pageable.next(), salesforceId);
            }
            return true;
        } catch (Exception e) {
            LOG.error("Error refreshing user authorities for salesforce id {}", salesforceId, e);
            return false;
        }
    }

    private List<String> hashBackupCodes(List<String> backupCodes) {
        return backupCodes.stream().map(passwordEncoder::encode).collect(Collectors.toList());
    }

    private List<String> generateBackupCodes() {
        List<String> backupCodes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_BATCH_SIZE; i++) {
            backupCodes.add(RandomStringUtils.random(BACKUP_CODE_LENGTH, true, true));
        }
        return backupCodes;
    }

    private boolean validLong(String code) {
        try {
            Long.parseLong(code);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
