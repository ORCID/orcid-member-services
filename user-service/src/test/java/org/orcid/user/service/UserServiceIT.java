package org.orcid.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orcid.user.UserServiceApp;
import org.orcid.user.domain.User;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.security.AuthoritiesConstants;
import org.orcid.user.service.util.RandomUtil;
import org.orcid.user.web.rest.errors.ExpiredKeyException;
import org.orcid.user.web.rest.errors.InvalidKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for {@link UserService}.
 */
@SpringBootTest(classes = UserServiceApp.class)
public class UserServiceIT {

    private static final String DEFAULT_EMAIL = "johndoe@orcid.org";

    private static final String DEFAULT_FIRSTNAME = "john";

    private static final String DEFAULT_LASTNAME = "doe";

    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";

    private static final String DEFAULT_LANGKEY = "dummy";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    public void assertThatUserMustExistToResetPassword() {
        User user = getUser();
        user.setActivated(true);
        userRepository.save(user);
        Optional<User> maybeUser = userService.requestPasswordReset("invalid.login@orcid.org");
        assertThat(maybeUser).isNotPresent();

        maybeUser = userService.requestPasswordReset(user.getEmail());
        assertThat(maybeUser).isPresent();
        assertThat(maybeUser.orElse(null).getEmail()).isEqualTo(user.getEmail());
        assertThat(maybeUser.orElse(null).getResetDate()).isNotNull();
        assertThat(maybeUser.orElse(null).getResetKey()).isNotNull();

        removeUser(user);
    }

    @Test
    public void assertThatOnlyActivatedUserCanRequestPasswordReset() {
        User user = getUser();
        user.setActivated(false);
        userRepository.save(user);

        Optional<User> maybeUser = userService.requestPasswordReset(user.getEmail());
        assertThat(maybeUser).isNotPresent();
        removeUser(user);
    }

    @Test
    public void assertThatResetKeyMustNotBeOlderThan24Hours() {
        User user = getUser();
        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        String resetKey = RandomUtil.generateResetKey();
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);
        userRepository.save(user);

        Assertions.assertThrows(ExpiredKeyException.class, () -> {
            userService.completePasswordReset("johndoe2", user.getResetKey());
        });
        removeUser(user);
    }

    @Test
    public void assertThatResetKeyMustBeValid() {
        User user = getUser();
        user.setActivated(true);
        user.setResetDate(null);
        user.setResetKey(null);
        userRepository.save(user);

        Assertions.assertThrows(InvalidKeyException.class, () -> {
            userService.completePasswordReset("johndoe2", "wrongkey");
        });
        removeUser(user);
    }

    @Test
    public void assertThatOldKeyRecognised() {
        User user = getUser();
        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        String resetKey = RandomUtil.generateResetKey();
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);
        userRepository.save(user);

        assertThat(userService.expiredResetKey(resetKey)).isTrue();
        removeUser(user);
    }

    @Test
    public void assertThatInvalidKeyRecognised() {
        User user = getUser();
        user.setActivated(true);
        user.setResetDate(null);
        user.setResetKey(null);
        userRepository.save(user);

        assertThat(userService.validResetKey("wrongkey")).isFalse();
        removeUser(user);
    }

    @Test
    public void assertThatUserCanResetPassword() throws ExpiredKeyException, InvalidKeyException {
        User user = getUser();
        Instant daysAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        String resetKey = RandomUtil.generateResetKey();
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);
        userRepository.save(user);

        userService.completePasswordReset("johndoe2", user.getResetKey());
        removeUser(user);
    }

    @Test
    public void assertThatNotActivatedUsersWithNotNullActivationKeyCreatedBefore3DaysAreDeleted() {
        User user = getUser();
        Instant now = Instant.now();
        user.setActivated(false);
        user.setActivationKey(RandomStringUtils.random(20));
        User dbUser = userRepository.save(user);
        dbUser.setCreatedDate(now.minus(4, ChronoUnit.DAYS));
        userRepository.save(user);
        List<User> users = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS));
        assertThat(users).isNotEmpty();
        userService.removeNotActivatedUsers();
        users = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS));
        assertThat(users).isEmpty();
        removeUser(user);
    }

    @Test
    public void assertThatNotActivatedUsersWithNullActivationKeyCreatedBefore3DaysAreNotDeleted() {
        User user = getUser();
        Instant now = Instant.now();
        user.setActivated(false);
        User dbUser = userRepository.save(user);
        dbUser.setCreatedDate(now.minus(4, ChronoUnit.DAYS));
        userRepository.save(user);
        List<User> users = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS));
        assertThat(users).isEmpty();
        userService.removeNotActivatedUsers();
        Optional<User> maybeDbUser = userRepository.findById(dbUser.getId());
        assertThat(maybeDbUser).contains(dbUser);
        removeUser(user);
    }

    private User getUser() {
        User user = new User();
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setAuthorities(Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));
        return user;
    }

    private void removeUser(User user) {
        userRepository.delete(user);
    }

}
