package org.orcid.memberportal.service.user.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.memberportal.service.user.config.ApplicationProperties;
import org.orcid.memberportal.service.user.domain.ActivationReminder;
import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.dto.UserDTO;
import org.orcid.memberportal.service.user.mapper.UserMapper;
import org.orcid.memberportal.service.user.repository.AuthorityRepository;
import org.orcid.memberportal.service.user.repository.UserRepository;
import org.orcid.memberportal.service.user.security.AuthoritiesConstants;
import org.orcid.memberportal.service.user.security.EncryptUtil;
import org.orcid.memberportal.service.user.security.MfaAuthenticationFailureException;
import org.orcid.memberportal.service.user.security.MfaSetup;
import org.orcid.memberportal.service.user.security.MockSecurityContext;
import org.orcid.memberportal.service.user.upload.UserUpload;
import org.orcid.memberportal.service.user.upload.UserUploadReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private UserUploadReader usersUploadReader;

    @Mock
    private MemberService memberService;

    @Mock
    private MailService mailService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ApplicationProperties applicationProperties;
    
    @Mock
    private EncryptUtil encryptUtil;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SecurityContextHolder.setContext(new MockSecurityContext("username"));
    }

    @Test
    void testResendActivationEmail() {
        User user = new User();
        user.setResetKey("key");
        user.setResetDate(Instant.now().minusSeconds(UserService.RESET_KEY_LIFESPAN_IN_SECONDS + 10000));
        Mockito.when(userRepository.findOneByResetKey(Mockito.eq("key"))).thenReturn(Optional.of(user));
        Mockito.doNothing().when(mailService).sendActivationEmail(Mockito.any(User.class));

        userService.resendActivationEmail("key");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository, Mockito.times(1)).save(captor.capture());
        Mockito.verify(mailService, Mockito.times(1)).sendActivationEmail(Mockito.any(User.class));

        User updated = captor.getValue();
        assertThat(updated.getResetKey()).isNotNull();
        assertThat(updated.getResetDate()).isNotNull();
        assertThat(updated.getResetKey()).isNotEqualTo("key");
    }

    @Test
    void testClearUser() {
        User userToBeCleared = new User();
        userToBeCleared.setId("some-id");
        userToBeCleared.setFirstName("first name");
        userToBeCleared.setLastName("last name");
        userToBeCleared.setEmail("something@somewhere.com");

        Mockito.when(userRepository.findOneById(Mockito.eq("some-id"))).thenReturn(Optional.of(userToBeCleared));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(null);

        userService.clearUser("some-id");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).save(captor.capture());
        User cleared = captor.getValue();
        assertNull(cleared.getFirstName());
        assertNull(cleared.getLastName());
        assertEquals("some-id@deleted.orcid.org", cleared.getEmail());
    }

    @Test
    void testCreateUserForMemberWithAsserionsEnabled() {
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });
        Mockito.doNothing().when(mailService).sendActivationEmail(Mockito.any(User.class));
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString())).thenReturn(true);
        Mockito.when(userMapper.toUser(Mockito.any(UserDTO.class))).thenReturn(new User());

        UserDTO userDTO = getUserDTO();
        userService.createUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        Mockito.verify(mailService, Mockito.times(1)).sendActivationEmail(Mockito.any(User.class));
        Mockito.verify(memberService, Mockito.times(1)).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());
        Mockito.verify(userMapper, Mockito.times(1)).toUser(Mockito.any(UserDTO.class));
    }

    @Test
    void testCreateUserForMemberWithoutAsserionsEnabled() {
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });
        Mockito.doNothing().when(mailService).sendActivationEmail(Mockito.any(User.class));
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString())).thenReturn(false);
        Mockito.when(userMapper.toUser(Mockito.any(UserDTO.class))).thenReturn(new User());

        UserDTO userDTO = getUserDTO();
        userService.createUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        Mockito.verify(mailService, Mockito.times(1)).sendActivationEmail(Mockito.any(User.class));
        Mockito.verify(memberService, Mockito.times(1)).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());
        Mockito.verify(userMapper, Mockito.times(1)).toUser(Mockito.any(UserDTO.class));
    }

    @Test
    void testUpdateUserForMemberWithAsserionsEnabled() {
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });

        User existing = new User();
        existing.setId("id");
        existing.setEmail("email@email.com");
        existing.setMainContact(false);

        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString())).thenReturn(true);
        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        Mockito.when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(new UserDTO());

        UserDTO userDTO = getUserDTO();
        userDTO.setId("id");
        userDTO.setMainContact(false);
        userService.updateUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(userCaptor.capture());
        Mockito.verify(memberService, Mockito.times(1)).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertTrue(user.getAuthorities().contains(AuthoritiesConstants.USER));
        assertTrue(user.getAuthorities().contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED));
    }

    @Test
    void testUpdateUserForMemberWithoutAsserionsEnabled() {
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });
        Mockito.doNothing().when(mailService).sendActivationEmail(Mockito.any(User.class));
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString())).thenReturn(false);

        User existing = new User();
        existing.setId("id");
        existing.setEmail("email@orcid.org");
        existing.setMainContact(false);

        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        Mockito.when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(new UserDTO());

        UserDTO userDTO = getUserDTO();
        userDTO.setId("id");
        userDTO.setEmail("email@orcid.org");
        userDTO.setMainContact(false);
        userService.updateUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(userCaptor.capture());
        Mockito.verify(memberService, Mockito.times(1)).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertTrue(user.getAuthorities().contains(AuthoritiesConstants.USER));
        assertFalse(user.getAuthorities().contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED));
    }

    @Test
    public void testUpdateUserNoOrgOwnerChange() {
        UserDTO toUpdate = new UserDTO();
        toUpdate.setMainContact(false);
        toUpdate.setEmail("email@orcid.org");
        toUpdate.setId("some-id");

        User existing = new User();
        existing.setMainContact(false);
        existing.setId("some-id");

        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        Mockito.when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(new UserDTO());

        userService.updateUser(toUpdate);

        Mockito.verify(mailService, Mockito.never()).sendOrganizationOwnerChangedMail(Mockito.any(User.class), Mockito.anyString());
    }

    @Test
    public void testUpdateUserWithOrgOwnerChange() {
        UserDTO toUpdate = new UserDTO();
        toUpdate.setMainContact(true);
        toUpdate.setId("some-id");
        toUpdate.setSalesforceId("salesforce");
        toUpdate.setEmail("email@orcid.org");

        User existing = new User();
        existing.setMainContact(false);
        existing.setId("some-id");
        existing.setSalesforceId("salesforce");

        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        Mockito.when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(new UserDTO());
        Mockito.when(memberService.getMemberNameBySalesforce(Mockito.anyString())).thenReturn("member");

        userService.updateUser(toUpdate);

        Mockito.verify(mailService, Mockito.times(1)).sendOrganizationOwnerChangedMail(Mockito.any(User.class), Mockito.anyString());
    }

    @Test
    public void testUpdateAccount() {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        Mockito.when(authentication.getPrincipal()).thenReturn("some@email.com");

        User user = new User();
        user.setFirstName("old first name");
        user.setLastName("old last name");
        user.setEmail("some@email.com");
        user.setAuthorities(new HashSet<>(Arrays.asList("ROLE_USER", "ROLE_ADMIN")));
        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(user));

        userService.updateAccount("new first name", "new last name", "no@change.com", "en", "hmmmm");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("new first name", updatedUser.getFirstName());
        assertEquals("new last name", updatedUser.getLastName());
        assertEquals("en", updatedUser.getLangKey());
        assertEquals("hmmmm", updatedUser.getImageUrl());
        assertEquals("some@email.com", updatedUser.getEmail());

        String[] authorities = updatedUser.getAuthorities().toArray(new String[0]);
        assertEquals(2, authorities.length); // no change to authorities
        assertTrue(AuthoritiesConstants.ADMIN.equals(authorities[0]) || AuthoritiesConstants.ADMIN.equals(authorities[1]));
        assertTrue(AuthoritiesConstants.USER.equals(authorities[0]) || AuthoritiesConstants.USER.equals(authorities[1]));
    }

    @Test
    public void testUploadUserCSV() throws IOException {
        Mockito.when(usersUploadReader.readUsersUpload(Mockito.any(InputStream.class), Mockito.any(User.class))).thenReturn(getUserUpload());
        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.eq("user1@orcid.org"))).thenReturn(Optional.empty());
        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.eq("user2@orcid.org"))).thenReturn(Optional.of(getUser("user2@orcid.org")));
        Mockito.when(userRepository.findOneById(Mockito.eq("user2@orcid.org"))).thenReturn(Optional.of(getUser("user2@orcid.org")));
        Mockito.when(userMapper.toUser(Mockito.any(UserDTO.class))).thenReturn(new User());

        InputStream inputStream = Mockito.mock(InputStream.class);
        userService.uploadUserCSV(inputStream, getUser("some-user@orcid.org"));

        // check only new users saved
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        Mockito.verify(userMapper, Mockito.times(1)).toUser(Mockito.any(UserDTO.class));
    }

    @Test
    public void testExpiredResetKey() {
        User user = new User();
        user.setResetDate(Instant.now().minusSeconds(UserService.RESET_KEY_LIFESPAN_IN_SECONDS - 2000));
        Mockito.when(userRepository.findOneByResetKey(Mockito.anyString())).thenReturn(Optional.of(user));
        assertFalse(userService.expiredResetKey("anything"));

        user.setResetDate(Instant.now().minusSeconds(UserService.RESET_KEY_LIFESPAN_IN_SECONDS + 2000));
        assertTrue(userService.expiredResetKey("anything"));
    }

    @Test
    public void testValidResetKey() {
        Mockito.when(userRepository.findOneByResetKey(Mockito.anyString())).thenReturn(Optional.of(new User()));
        assertTrue(userService.validResetKey("anything"));

        Mockito.when(userRepository.findOneByResetKey(Mockito.anyString())).thenReturn(Optional.empty());
        assertFalse(userService.validResetKey("anything"));
    }

    @Test
    public void testUpdateMemberNames_fullPage() {
        Mockito.when(userRepository.findByMemberName(Mockito.any(Pageable.class), Mockito.isNull())).thenReturn(new PageImpl<>(getListOfUsers(10)));
        Mockito.when(memberService.getMemberNameBySalesforce(Mockito.anyString())).thenReturn("member name");

        userService.updateMemberNames();

        Mockito.verify(userRepository, Mockito.times(1)).findByMemberName(Mockito.any(Pageable.class), Mockito.isNull());
        Mockito.verify(memberService, Mockito.times(10)).getMemberNameBySalesforce(Mockito.anyString());
    }

    @Test
    public void testUpdateMemberNames_partPage() {
        Mockito.when(userRepository.findByMemberName(Mockito.any(Pageable.class), Mockito.isNull())).thenReturn(new PageImpl<>(getListOfUsers(2)));
        Mockito.when(memberService.getMemberNameBySalesforce(Mockito.anyString())).thenReturn("member name");

        userService.updateMemberNames();

        Mockito.verify(userRepository, Mockito.times(1)).findByMemberName(Mockito.any(Pageable.class), Mockito.isNull());
        Mockito.verify(memberService, Mockito.times(2)).getMemberNameBySalesforce(Mockito.anyString());
    }

    @Test
    public void testGetAllManagedUsers() {
        Mockito.when(userRepository.findByDeletedFalse(Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(getListOfUsers(20)));
        Mockito.when(userRepository
                .findByDeletedIsFalseAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndEmailContainingIgnoreCase(
                        Mockito.eq("filter"), Mockito.eq("filter"), Mockito.eq("filter"), Mockito.eq("filter"), Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(getListOfUsers(10)));

        Page<UserDTO> users = userService.getAllManagedUsers(Mockito.mock(Pageable.class));
        assertNotNull(users);
        assertEquals(20, users.getTotalElements());

        Mockito.verify(userRepository, Mockito.times(1)).findByDeletedFalse(Mockito.any(Pageable.class));
        Mockito.verify(userMapper, Mockito.times(20)).toUserDTO(Mockito.any(User.class));

        users = userService.getAllManagedUsers(Mockito.mock(Pageable.class), "filter");
        assertNotNull(users);
        assertEquals(10, users.getTotalElements());

        Mockito.verify(userRepository, Mockito.times(1))
                .findByDeletedIsFalseAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndEmailContainingIgnoreCase(
                        Mockito.eq("filter"), Mockito.eq("filter"), Mockito.eq("filter"), Mockito.eq("filter"), Mockito.any(Pageable.class));
        Mockito.verify(userMapper, Mockito.times(30)).toUserDTO(Mockito.any(User.class)); // 10
                                                                                          // more
    }

    @Test
    public void testGetCurrentUser() {
        User impersonatingUser = new User();
        impersonatingUser.setEmail("admin-user");
        impersonatingUser.setLoginAs("impersonated-user");

        User impersonatedUser = new User();
        impersonatedUser.setEmail("impersonated-user");

        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.eq("username"))).thenReturn(Optional.of(impersonatingUser));
        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.eq("impersonated-user"))).thenReturn(Optional.of(impersonatedUser));

        User user = userService.getCurrentUser();
        assertEquals("impersonated-user", user.getEmail());

        impersonatingUser.setLoginAs(null);
        user = userService.getCurrentUser();
        assertEquals("admin-user", user.getEmail());
    }

    @Test
    public void testGetAllUsersBySalesforceId() {
        Mockito.when(userRepository.findBySalesforceIdAndDeletedIsFalse(Mockito.any(Pageable.class), Mockito.anyString())).thenReturn(new PageImpl<>(getListOfUsers(10)));
        Mockito.when(userRepository
                .findByDeletedIsFalseAndSalesforceIdAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndSalesforceIdAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndSalesforceIdAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndSalesforceIdAndEmailContainingIgnoreCase(
                        Mockito.any(Pageable.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new PageImpl<>(getListOfUsers(5)));

        Page<UserDTO> page = userService.getAllUsersBySalesforceId(Mockito.mock(Pageable.class), "some-salesforce-id");
        assertEquals(10, page.getTotalElements());

        page = userService.getAllUsersBySalesforceId(Mockito.mock(Pageable.class), "some-salesforce-id", "some-filter");
        assertEquals(5, page.getTotalElements());
    }

    @Test
    public void testSendActivationReminders() {
        // user not due any reminders
        User user1 = getUser("1@user.com");
        user1.setCreatedDate(LocalDateTime.now().minusWeeks(2).atZone(ZoneId.systemDefault()).toInstant());

        List<ActivationReminder> user1Reminders = new ArrayList<>();
        user1Reminders.add(new ActivationReminder(7, Instant.now()));
        user1.setActivationReminders(user1Reminders);

        // user due 30 day reminder
        User user2 = getUser("2@user.com");
        user2.setCreatedDate(LocalDateTime.now().minusWeeks(5).atZone(ZoneId.systemDefault()).toInstant());

        List<ActivationReminder> user2Reminders = new ArrayList<>();
        user2Reminders.add(new ActivationReminder(7, Instant.now()));
        user2.setActivationReminders(user2Reminders);

        // user due 7 day reminder
        User user3 = getUser("3@user.com");
        user3.setCreatedDate(LocalDateTime.now().minusDays(8).atZone(ZoneId.systemDefault()).toInstant());

        // user not due any reminders
        User user4 = getUser("4@user.com");
        user4.setCreatedDate(LocalDateTime.now().minusDays(3).atZone(ZoneId.systemDefault()).toInstant());

        // user not due any reminders
        User user5 = getUser("5@user.com");
        user5.setCreatedDate(LocalDateTime.now().minusWeeks(5).atZone(ZoneId.systemDefault()).toInstant());

        List<ActivationReminder> user5Reminders = new ArrayList<>();
        user5Reminders.add(new ActivationReminder(7, Instant.now()));
        user5Reminders.add(new ActivationReminder(30, Instant.now()));
        user5.setActivationReminders(user5Reminders);

        Mockito.when(userRepository.findAllByActivatedIsFalseAndDeletedIsFalse()).thenReturn(Arrays.asList(user1, user2, user3, user4, user5));

        userService.sendActivationReminders();

        Mockito.verify(userRepository, Mockito.times(4)).save(userCaptor.capture());

        List<User> captured = userCaptor.getAllValues();
        assertThat(captured.get(1).getEmail()).isEqualTo("2@user.com");
        assertThat(captured.get(1).getActivationReminders().size()).isEqualTo(2);
        assertThat(captured.get(1).getActivationReminders().get(0).getDaysElapsed()).isEqualTo(7);
        assertThat(captured.get(1).getActivationReminders().get(1).getDaysElapsed()).isEqualTo(30);
        assertThat(captured.get(3).getEmail()).isEqualTo("3@user.com");
        assertThat(captured.get(3).getActivationReminders().size()).isEqualTo(1);
        assertThat(captured.get(3).getActivationReminders().get(0).getDaysElapsed()).isEqualTo(7);
    }

    @Test
    public void testGetMfaSetup() {
        MfaSetup mfaSetup = userService.getMfaSetup();
        assertThat(mfaSetup.getSecret()).isNotNull();
        assertThat(mfaSetup.getOtp()).isNull();
        assertThat(mfaSetup.getQrCode()).isNotNull();
    }

    @Test
    public void testDisableMfa() {
        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.eq("username"))).thenReturn(Optional.of(getUserUsingMfa()));
        userService.disableMfa();
        Mockito.verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertThat(captured.getMfaEnabled()).isFalse();
        assertThat(captured.getMfaEncryptedSecret()).isNull();
        assertThat(captured.getMfaBackupCodes()).isNull();
    }

    @Test
    public void testValidateOtp() {
        String secret = Base32.random();
        Totp totp = new Totp(secret);
        String code = totp.now();
        userService.validateOtp(code, secret);

        Assertions.assertThrows(MfaAuthenticationFailureException.class, () -> {
            userService.validateOtp("non-numerical-code", secret);
        });

        Assertions.assertThrows(MfaAuthenticationFailureException.class, () -> {
            userService.validateOtp("123456", secret);
        });
    }
    
    @Test
    public void testValidMfaCode() {
        String backupCode1 = "backupCode1";
        String backupCode2 = "backupCode2";
        String backupCode3 = "backupCode3";
        
        Mockito.when(passwordEncoder.matches(Mockito.eq("backupCode2"), Mockito.eq("backupCode2"))).thenReturn(true);
        
        User user = new User();
        user.setEmail("user");
        user.setMfaEnabled(true);
        user.setMfaEncryptedSecret("some secret");
        user.setMfaBackupCodes(Arrays.asList(backupCode1, backupCode2, backupCode3));
        
        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.eq("user"))).thenReturn(Optional.of(user));
        userService.validMfaCode("user", "backupCode2");
        
        Mockito.verify(passwordEncoder, Mockito.times(3)).matches(Mockito.eq("backupCode2"), Mockito.anyString());
        Mockito.verify(userRepository).findOneByEmailIgnoreCase(Mockito.eq("user"));
        Mockito.verify(userRepository).save(userCaptor.capture());
        
        User saved = userCaptor.getValue();
        assertEquals(2, saved.getMfaBackupCodes().size());
        assertFalse(saved.getMfaBackupCodes().contains("backupCode2"));
    }

    @Test
    public void testEnableMfa() {
        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.eq("username"))).thenReturn(Optional.of(getUser("username")));
        Mockito.when(encryptUtil.encrypt(Mockito.anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return "encrypted-" + (String) args[0];
            }
        });
        Mockito.when(passwordEncoder.encode(Mockito.anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return "encoded-" + (String) args[0];
            }
        });

        MfaSetup mfaSetup = new MfaSetup();
        String secret = Base32.random();
        mfaSetup.setSecret(secret);
        mfaSetup.setOtp("non-numerical-code");
        Assertions.assertThrows(MfaAuthenticationFailureException.class, () -> {
            userService.enableMfa(mfaSetup);
        });
        
        mfaSetup.setOtp("123456");
        Assertions.assertThrows(MfaAuthenticationFailureException.class, () -> {
            userService.enableMfa(mfaSetup);
        });
        
        Totp totp = new Totp(secret);
        String code = totp.now();
        mfaSetup.setOtp(code);
        
        List<String> backupCodes = userService.enableMfa(mfaSetup);
        
        assertThat(backupCodes).isNotNull();
        assertThat(backupCodes.size()).isEqualTo(UserService.BACKUP_CODE_BATCH_SIZE);
        backupCodes.forEach(c -> {
            assertThat(c.length()).isEqualTo(UserService.BACKUP_CODE_LENGTH); // check code has not been hashed
        });
        
        Mockito.verify(userRepository).save(userCaptor.capture());
        
        User captured = userCaptor.getValue();
        assertThat(captured.getMfaEnabled()).isTrue();
        assertThat(captured.getMfaEncryptedSecret()).isNotNull();
        assertThat(captured.getMfaEncryptedSecret()).isEqualTo("encrypted-" + secret);
        assertThat(captured.getMfaBackupCodes()).isNotNull();
        assertThat(captured.getMfaBackupCodes().size()).isEqualTo(UserService.BACKUP_CODE_BATCH_SIZE);
        captured.getMfaBackupCodes().forEach(c -> {
            assertThat(c).startsWith("encoded-"); // check code has been hashed
        });
    }

    private User getUserUsingMfa() {
        User user = getUser("username");
        user.setMfaEnabled(true);
        user.setMfaEncryptedSecret("some encrypted secret");
        user.setMfaBackupCodes(Arrays.asList("backup1", "backup2", "backupn"));
        return user;
    }

    private List<User> getListOfUsers(int size) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            users.add(getUser(String.valueOf(i)));
        }
        return users;
    }

    private User getUser(String login) {
        User user = new User();
        user.setEmail(login);
        user.setId(login);
        user.setSalesforceId(login);
        return user;
    }

    private UserUpload getUserUpload() {
        UserUpload upload = new UserUpload();
        UserDTO user1 = new UserDTO();
        user1.setEmail("user1@orcid.org");

        UserDTO user2 = new UserDTO();
        user2.setEmail("user2@orcid.org");

        upload.addUserDTO(user1);
        upload.addUserDTO(user2);

        return upload;
    }

    private UserDTO getUserDTO() {
        UserDTO user = new UserDTO();
        user.setActivated(false);
        user.setEmail("email@email.com");
        user.setMainContact(false);
        user.setFirstName("first");
        user.setLastName("last");
        user.setSalesforceId("member");
        user.setIsAdmin(false);
        user.setAuthorities(Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));
        return user;
    }

}
