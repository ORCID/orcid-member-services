package org.orcid.mp.user.rest;

import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.orcid.mp.user.config.Constants;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.PasswordChangeDTO;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.mapper.UserMapper;
import org.orcid.mp.user.repository.UserRepository;

import org.orcid.mp.user.rest.error.*;
import org.orcid.mp.user.rest.vm.KeyAndPasswordVM;
import org.orcid.mp.user.rest.vm.KeyVM;
import org.orcid.mp.user.rest.vm.MfaSetup;
import org.orcid.mp.user.rest.vm.PasswordResetResultVM;
import org.orcid.mp.user.service.MailService;
import org.orcid.mp.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the current user's account.
 */
@RestController
@RequestMapping("/api")
public class AccountResource {

    private final Logger log = LoggerFactory.getLogger(AccountResource.class);

    public static final String ROLE_PREVIOUS_ADMINISTRATOR = "ROLE_PREVIOUS_ADMINISTRATOR";

    protected final UserRepository userRepository;

    protected final UserService userService;

    protected final UserMapper userMapper;

    protected final MailService mailService;

    public AccountResource(UserRepository userRepository, UserService userService, MailService mailService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
        this.userMapper = userMapper;
    }

    /**
     * {@code GET  /authenticate} : check if the user is authenticated, and
     * return its login.
     *
     * @param request the HTTP request.
     * @return the login if the user is authenticated.
     */
    @GetMapping("/authenticate")
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * {@code POST  /account} : update the current user information.
     *
     * @param userDTO the current user information.
     * @throws EmailAlreadyUsedException {@code 400 (Bad Request)} if the email is already used.
     * @throws RuntimeException          {@code 500 (Internal Server Error)} if the user login wasn't
     *                                   found.
     */
    @PostMapping("/account")
    public void saveAccount(@Valid @RequestBody UserDTO userDTO) {
        User currentUser = userService.getCurrentUser();
        if (!currentUser.getEmail().equalsIgnoreCase(userDTO.getEmail())) {
            throw new BadRequestAlertException("Updating email is not allowed");
        }

        userService.updateAccount(userDTO.getFirstName(), userDTO.getLastName(), userDTO.getEmail(), userDTO.getLangKey(), userDTO.getImageUrl());
    }

    /**
     * {@code GET  /account} : get the current user.
     *
     * @return the current user.
     * @throws RuntimeException {@code 500 (Internal Server Error)} if the user couldn't be
     *                          returned.
     */
    @GetMapping("/account")
    public UserDTO getAccount() {
        User user = userService.getCurrentUser();
        return userMapper.toUserDTO(user);
    }

    /**
     * {@code GET  /account/mfa} : get a secret to set up mfa for the current
     * user
     *
     * @return object containing mfa which can then be used to submit otp.
     */
    @GetMapping("/account/mfa")
    public ResponseEntity<MfaSetup> getMfaSetup() {
        MfaSetup mfaSetup = userService.getMfaSetup();
        return ResponseEntity.ok(mfaSetup);
    }

    /**
     * {@code POST  /account/mfa} : enables mfa for the current user, if the
     * supplied otp matches the secret
     *
     * @param mfaSetup - the otp and secret
     */
    @PostMapping(path = "/account/mfa/on")
    public ResponseEntity<List<String>> switchOnMfa(@RequestBody MfaSetup mfaSetup) {
        if (mfaSetup == null || StringUtils.isBlank(mfaSetup.getOtp())) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<String> backupCodes = userService.enableMfa(mfaSetup);
            return ResponseEntity.ok(backupCodes);
        } catch (MfaAuthenticationFailureException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * {@code POST  /account/mfa} : disables mfa for the specified user
     */
    @PostMapping(path = "/account/{userId}/mfa/off")
    public ResponseEntity<Void> switchOffMfa(@PathVariable String userId) {
        userService.disableMfa(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code POST  /account/change-password} : changes the current user's
     * password.
     *
     * @param passwordChangeDto current and new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(path = "/account/change-password")
    public void changePassword(@RequestBody PasswordChangeDTO passwordChangeDto) {
        if (!checkPasswordLength(passwordChangeDto.getNewPassword())) {
            throw new InvalidPasswordException();
        }
        userService.changePassword(passwordChangeDto.getCurrentPassword(), passwordChangeDto.getNewPassword());
    }

    /**
     * {@code POST   /account/reset-password/init} : Send an email to reset the
     * password of the user.
     *
     * @param mail the mail of the user.
     * @throws EmailNotFoundException {@code 400 (Bad Request)} if the email address is not
     *                                registered.
     */
    @PostMapping(path = "/account/reset-password/init")
    public void requestPasswordReset(@RequestBody String mail) {
        mailService.sendPasswordResetMail(userService.requestPasswordReset(mail).orElseThrow(EmailNotFoundException::new));
    }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the
     * password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal Server Error)} if the password could not
     *                                  be reset.
     */
    @PostMapping(path = "/account/reset-password/finish")
    public ResponseEntity<PasswordResetResultVM> finishPasswordReset(@RequestBody KeyAndPasswordVM keyAndPassword) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            throw new InvalidPasswordException();
        }

        PasswordResetResultVM result = new PasswordResetResultVM();
        try {
            userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());
            result.setSuccess(true);
        } catch (ExpiredKeyException e) {
            result.setExpiredKey(true);
        } catch (InvalidKeyException e) {
            result.setInvalidKey(true);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * {@code POST   /account/reset-password/key/validate} : validate a reset
     * password key
     *
     * @throws InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal Server Error)} if the password could not
     *                                  be reset.
     */
    @PostMapping(path = "/account/reset-password/validate")
    public ResponseEntity<PasswordResetResultVM> validateKey(@RequestBody KeyVM key) {
        PasswordResetResultVM result = new PasswordResetResultVM();
        if (userService.validResetKey(key.getKey())) {
            result.setExpiredKey(userService.expiredResetKey(key.getKey()));
        } else {
            result.setInvalidKey(true);
        }
        return ResponseEntity.ok(result);
    }

    protected static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) && password.length() >= Constants.PASSWORD_MIN_LENGTH && password.length() <= Constants.PASSWORD_MAX_LENGTH;
    }
}
