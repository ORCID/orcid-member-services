package org.orcid.mp.user.service;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.mp.user.client.MemberServiceClient;
import org.orcid.mp.user.config.togglz.PortalFeatures;
import org.orcid.mp.user.domain.ActivationReminder;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.mapper.UserMapper;
import org.orcid.mp.user.repository.UserRepository;
import org.orcid.mp.user.error.BadRequestAlertException;
import org.orcid.mp.user.error.MfaAuthenticationFailureException;
import org.orcid.mp.user.pojo.MfaSetup;
import org.orcid.mp.user.security.AuthoritiesConstants;
import org.orcid.mp.user.security.EncryptUtil;
import org.orcid.mp.user.security.MockSecurityContext;
import org.orcid.mp.user.upload.UserCsvReader;
import org.orcid.mp.user.upload.UserUpload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.togglz.testing.TestFeatureManager;
import org.togglz.testing.TestFeatureManagerProvider;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserCsvReader usersUploadReader;

    @Mock
    private MemberServiceClient memberServiceClient;

    @Mock
    private MailService mailService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EncryptUtil encryptUtil;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @InjectMocks
    private UserService userService;

    private TestFeatureManager featureManager;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SecurityContextHolder.setContext(new MockSecurityContext("username"));
        featureManager = new TestFeatureManager(PortalFeatures.class);
        featureManager.disableAll();
        TestFeatureManagerProvider.setFeatureManager(featureManager);
    }

    @Test
    void testResendActivationEmail() {
        User user = new User();
        user.setResetKey("key");
        user.setResetDate(Instant.now().minusSeconds(UserService.RESET_KEY_LIFESPAN_IN_SECONDS + 10000));
        when(userRepository.findOneByResetKey(eq("key"))).thenReturn(Optional.of(user));
        Mockito.doNothing().when(mailService).sendActivationEmail(Mockito.any(User.class));

        userService.resendActivationEmail("key");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, Mockito.times(1)).save(captor.capture());
        verify(mailService, Mockito.times(1)).sendActivationEmail(Mockito.any(User.class));

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

        when(userRepository.findOneById(eq("some-id"))).thenReturn(Optional.of(userToBeCleared));
        when(userRepository.save(Mockito.any(User.class))).thenReturn(null);

        userService.clearUser("some-id");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User cleared = captor.getValue();
        assertNull(cleared.getFirstName());
        assertNull(cleared.getLastName());
        assertEquals("some-id@deleted.orcid.org", cleared.getEmail());
    }

    @Test
    void testCreateUserForMemberWithAsserionsEnabled() {
        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithAMEnabled());
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });
        Mockito.doNothing().when(mailService).sendActivationEmail(Mockito.any(User.class));
        when(userMapper.toUser(Mockito.any(UserDTO.class))).thenReturn(new User());

        UserDTO userDTO = getUserDTO();
        userService.createUser(userDTO);

        verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        verify(mailService, Mockito.times(1)).sendActivationEmail(Mockito.any(User.class));
        verify(userMapper, Mockito.times(1)).toUser(Mockito.any(UserDTO.class));
    }

    @Test
    void testCreateUserForMemberApiCredsEnabled() {
        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithAMEnabled());
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });
        Mockito.doNothing().when(mailService).sendActivationEmail(Mockito.any(User.class));
        Mockito.doNothing().when(mailService).sendApiCredsEnabledEmail(Mockito.any(User.class));

        User mappedUser = new User();
        mappedUser.setManageApiCredsEnabled(true);
        when(userMapper.toUser(Mockito.any(UserDTO.class))).thenReturn(mappedUser);

        UserDTO userDTO = getUserDTO();
        userDTO.setManageApiCredsEnabled(true);
        userService.createUser(userDTO);

        verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        verify(mailService, Mockito.times(1)).sendActivationEmail(Mockito.any(User.class));
        verify(mailService, Mockito.times(1)).sendApiCredsEnabledEmail(Mockito.any(User.class));
        verify(userMapper, Mockito.times(1)).toUser(Mockito.any(UserDTO.class));
    }

    @Test
    void testCreateUserForMemberWithoutAsserionsEnabled() {
        featureManager.enableAll();

        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithAMEnabled());
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });
        Mockito.doNothing().when(mailService).sendActivationEmail(Mockito.any(User.class));
        when(userMapper.toUser(Mockito.any(UserDTO.class))).thenReturn(new User());

        UserDTO userDTO = getUserDTO();
        userService.createUser(userDTO);

        verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        verify(mailService, Mockito.times(1)).sendActivationEmail(Mockito.any(User.class));
    }

    @Test
    void testUpdateUserForMemberWithAsserionsEnabled() {
        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithAMEnabled());
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });

        User existing = new User();
        existing.setId("id");
        existing.setEmail("email@email.com");
        existing.setMainContact(false);

        when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(new UserDTO());

        UserDTO userDTO = getUserDTO();
        userDTO.setId("id");
        userDTO.setMainContact(false);
        userDTO.setIsAdmin(false);
        userService.updateUser(userDTO);

        verify(userRepository, Mockito.times(1)).save(userCaptor.capture());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertFalse(user.getAdmin());
    }

    @Test
    void testUpdateUserForMemberWithManageApiCredentialsEnabled() {
        featureManager.enableAll(); // enable togglz

        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithAMEnabled());
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });

        User existing = new User();
        existing.setId("id");
        existing.setEmail("email@email.com");
        existing.setMainContact(false);
        existing.setManageApiCredsEnabled(false);

        when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(new UserDTO());

        UserDTO userDTO = getUserDTO();
        userDTO.setId("id");
        userDTO.setMainContact(false);
        userDTO.setIsAdmin(false);
        userDTO.setManageApiCredsEnabled(true);
        userService.updateUser(userDTO);

        verify(userRepository, Mockito.times(1)).save(userCaptor.capture());
        verify(mailService, Mockito.times(1)).sendApiCredsEnabledEmail(userCaptor.capture());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertFalse(user.getAdmin());
        assertTrue(user.getManageApiCredsEnabled());
    }

    @Test
    void testUpdateUserForMemberWithManageApiCredentialsDisabled() {
        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithAMEnabled());
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });

        User existing = new User();
        existing.setId("id");
        existing.setEmail("email@email.com");
        existing.setMainContact(false);
        existing.setManageApiCredsEnabled(true);

        when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(new UserDTO());

        UserDTO userDTO = getUserDTO();
        userDTO.setId("id");
        userDTO.setMainContact(false);
        userDTO.setIsAdmin(false);
        userDTO.setManageApiCredsEnabled(false);
        userService.updateUser(userDTO);

        verify(userRepository, Mockito.times(1)).save(userCaptor.capture());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertFalse(user.getAdmin());
        assertFalse(user.getManageApiCredsEnabled());
    }

    @Test
    void testUpdateUserForMemberWithoutAsserionsEnabled() {
        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithoutAMEnabled());
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });
        Mockito.doNothing().when(mailService).sendActivationEmail(Mockito.any(User.class));

        User existing = new User();
        existing.setId("id");
        existing.setEmail("email@orcid.org");
        existing.setMainContact(false);

        when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(new UserDTO());

        UserDTO userDTO = getUserDTO();
        userDTO.setId("id");
        userDTO.setEmail("email@orcid.org");
        userDTO.setMainContact(false);
        userDTO.setIsAdmin(true);
        userService.updateUser(userDTO);

        verify(userRepository, Mockito.times(1)).save(userCaptor.capture());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertTrue(user.getAdmin());
    }

    @Test
    public void testUpdateUserNoOrgOwnerChange() {
        UserDTO toUpdate = new UserDTO();
        toUpdate.setMainContact(false);
        toUpdate.setEmail("email@orcid.org");
        toUpdate.setMemberId("member");
        toUpdate.setId("some-id");

        User existing = new User();
        existing.setMainContact(false);
        existing.setMemberId("member");
        existing.setId("some-id");

        UserDTO existingDTO = new UserDTO();
        existingDTO.setMainContact(false);
        existingDTO.setMemberId("member");

        when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(existingDTO);
        when(memberServiceClient.getMember(eq("member"))).thenReturn(memberWithAMEnabled());

        userService.updateUser(toUpdate);

        verify(mailService, Mockito.never()).sendOrganizationOwnerChangedMail(Mockito.any(User.class), Mockito.anyString());
    }

    @Test
    public void testUpdateUserWithOrgOwnerChange() {
        UserDTO toUpdate = new UserDTO();
        toUpdate.setMainContact(true);
        toUpdate.setId("some-id");
        toUpdate.setMemberId("member");
        toUpdate.setEmail("email@orcid.org");

        User existing = new User();
        existing.setMainContact(false);
        existing.setId("some-id");
        existing.setMemberId("member");

        UserDTO existingDTO = new UserDTO();
        existingDTO.setMainContact(false);
        existingDTO.setMemberId("member");

        when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(existing));
        when(userMapper.toUserDTO(Mockito.any(User.class))).thenReturn(existingDTO);
        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithAMEnabled());

        userService.updateUser(toUpdate);

        verify(mailService, Mockito.times(1)).sendOrganizationOwnerChangedMail(Mockito.any(User.class), Mockito.anyString());
    }

    @Test
    public void testUpdateAccount() {
        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getPrincipal()).thenReturn("some@email.com");

        User user = new User();
        user.setFirstName("old first name");
        user.setLastName("old last name");
        user.setEmail("some@email.com");
        when(userRepository.findOneByEmailIgnoreCase(Mockito.anyString())).thenReturn(Optional.of(user));

        userService.updateAccount("new first name", "new last name", "no@change.com", "en", "hmmmm");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("new first name", updatedUser.getFirstName());
        assertEquals("new last name", updatedUser.getLastName());
        assertEquals("en", updatedUser.getLangKey());
        assertEquals("hmmmm", updatedUser.getImageUrl());
        assertEquals("some@email.com", updatedUser.getEmail());
    }

    @Test
    public void testUploadUserCSV() throws IOException {
        when(memberServiceClient.getMember(eq("member-id"))).thenReturn(memberWithAMEnabled());
        when(usersUploadReader.readUsersUpload(Mockito.any(InputStream.class), Mockito.any(User.class))).thenReturn(getUserUpload());
        when(userRepository.findOneByEmailIgnoreCase(eq("user1@orcid.org"))).thenReturn(Optional.empty());
        when(userRepository.findOneByEmailIgnoreCase(eq("user2@orcid.org"))).thenReturn(Optional.of(getUser("user2@orcid.org")));
        when(userRepository.findOneById(eq("user2@orcid.org"))).thenReturn(Optional.of(getUser("user2@orcid.org")));
        when(userMapper.toUser(Mockito.any(UserDTO.class))).thenReturn(new User());

        InputStream inputStream = Mockito.mock(InputStream.class);
        userService.uploadUserCSV(inputStream, getUser("some-user@orcid.org"));

        // check only new users saved
        verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        verify(userMapper, Mockito.times(1)).toUser(Mockito.any(UserDTO.class));
    }

    @Test
    public void testExpiredResetKey() {
        User user = new User();
        user.setResetDate(Instant.now().minusSeconds(UserService.RESET_KEY_LIFESPAN_IN_SECONDS - 2000));
        when(userRepository.findOneByResetKey(Mockito.anyString())).thenReturn(Optional.of(user));
        assertFalse(userService.expiredResetKey("anything"));

        user.setResetDate(Instant.now().minusSeconds(UserService.RESET_KEY_LIFESPAN_IN_SECONDS + 2000));
        assertTrue(userService.expiredResetKey("anything"));
    }

    @Test
    public void testValidResetKey() {
        when(userRepository.findOneByResetKey(Mockito.anyString())).thenReturn(Optional.of(new User()));
        assertTrue(userService.validResetKey("anything"));

        when(userRepository.findOneByResetKey(Mockito.anyString())).thenReturn(Optional.empty());
        assertFalse(userService.validResetKey("anything"));
    }

    @Test
    public void testUpdateMemberNames_fullPage() {
        when(userRepository.findByMemberName(Mockito.any(Pageable.class), Mockito.isNull())).thenReturn(new PageImpl<>(getListOfUsers(10)));
        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithAMEnabled());

        userService.updateMemberNames();

        verify(userRepository, Mockito.times(1)).findByMemberName(Mockito.any(Pageable.class), Mockito.isNull());
        verify(memberServiceClient, Mockito.times(10)).getMember(Mockito.anyString());
    }

    @Test
    public void testUpdateMemberNames_partPage() {
        when(userRepository.findByMemberName(Mockito.any(Pageable.class), Mockito.isNull())).thenReturn(new PageImpl<>(getListOfUsers(2)));
        when(memberServiceClient.getMember(Mockito.anyString())).thenReturn(memberWithAMEnabled());

        userService.updateMemberNames();

        verify(userRepository, Mockito.times(1)).findByMemberName(Mockito.any(Pageable.class), Mockito.isNull());
        verify(memberServiceClient, Mockito.times(2)).getMember(Mockito.anyString());
    }

    @Test
    public void testGetAllManagedUsers() {
        when(userRepository.findByDeletedFalse(Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(getListOfUsers(20)));
        when(userRepository.findByDeletedIsFalseAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndEmailContainingIgnoreCase(eq("filter"), eq("filter"), eq("filter"), eq("filter"), Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(getListOfUsers(10)));

        Page<UserDTO> users = userService.getAllManagedUsers(Mockito.mock(Pageable.class));
        assertNotNull(users);
        assertEquals(20, users.getTotalElements());

        verify(userRepository, Mockito.times(1)).findByDeletedFalse(Mockito.any(Pageable.class));
        verify(userMapper, Mockito.times(20)).toUserDTO(Mockito.any(User.class));

        users = userService.getAllManagedUsers(Mockito.mock(Pageable.class), "filter");
        assertNotNull(users);
        assertEquals(10, users.getTotalElements());

        verify(userRepository, Mockito.times(1)).findByDeletedIsFalseAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndEmailContainingIgnoreCase(eq("filter"), eq("filter"), eq("filter"), eq("filter"), Mockito.any(Pageable.class));
        verify(userMapper, Mockito.times(30)).toUserDTO(Mockito.any(User.class)); // 10
        // more
    }

    @Test
    public void testGetAllUsersByMemberId() {
        when(userRepository.findByMemberIdAndDeletedIsFalse(Mockito.any(Pageable.class), Mockito.anyString())).thenReturn(new PageImpl<>(getListOfUsers(10)));
        when(userRepository.findByDeletedIsFalseAndMemberIdAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndMemberIdAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndMemberIdAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndMemberIdAndEmailContainingIgnoreCase(Mockito.any(Pageable.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(new PageImpl<>(getListOfUsers(5)));

        Page<UserDTO> page = userService.getAllUsersByMemberId(Mockito.mock(Pageable.class), "some-member-id");
        assertEquals(10, page.getTotalElements());

        page = userService.getAllUsersByMemberId(Mockito.mock(Pageable.class), "some-member-id", "some-filter");
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

        when(userRepository.findAllByActivatedIsFalseAndDeletedIsFalse()).thenReturn(Arrays.asList(user1, user2, user3, user4, user5));

        userService.sendActivationReminders();

        verify(userRepository, Mockito.times(4)).save(userCaptor.capture());

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
    public void testDisableMfa_currentUser() {
        when(userRepository.findOneByEmailIgnoreCase(eq("username"))).thenReturn(Optional.of(getUserUsingMfa()));
        when(userRepository.findById(eq("userId"))).thenReturn(Optional.of(getUserUsingMfa()));
        userService.disableMfa("userId");
        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertThat(captured.getMfaEnabled()).isFalse();
        assertThat(captured.getMfaEncryptedSecret()).isNull();
        assertThat(captured.getMfaBackupCodes()).isNull();
    }

    @Test
    public void testDisableMfa_adminUser() {
        when(userRepository.findOneByEmailIgnoreCase(eq("username"))).thenReturn(Optional.of(getAdminUser()));
        when(userRepository.findById(eq("userId"))).thenReturn(Optional.of(getUserUsingMfa()));
        userService.disableMfa("userId");
        verify(userRepository).save(userCaptor.capture());
        User captured = userCaptor.getValue();
        assertThat(captured.getMfaEnabled()).isFalse();
        assertThat(captured.getMfaEncryptedSecret()).isNull();
        assertThat(captured.getMfaBackupCodes()).isNull();
    }

    @Test
    public void testDisableMfa_notAdminUserOrCurrentUser() {
        when(userRepository.findOneByEmailIgnoreCase(eq("username"))).thenReturn(Optional.of(getNonAdminUser()));
        when(userRepository.findById(eq("userId"))).thenReturn(Optional.of(getUserUsingMfa()));

        assertThrows(BadRequestAlertException.class, () -> {
            userService.disableMfa("userId");
        });
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

        when(passwordEncoder.matches(eq("backupCode2"), eq("backupCode2"))).thenReturn(true);

        User user = new User();
        user.setEmail("user");
        user.setMfaEnabled(true);
        user.setMfaEncryptedSecret("some secret");
        user.setMfaBackupCodes(Arrays.asList(backupCode1, backupCode2, backupCode3));

        when(userRepository.findOneByEmailIgnoreCase(eq("user"))).thenReturn(Optional.of(user));
        userService.validMfaCode("user", "backupCode2");

        verify(passwordEncoder, Mockito.times(3)).matches(eq("backupCode2"), Mockito.anyString());
        verify(userRepository).findOneByEmailIgnoreCase(eq("user"));
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals(2, saved.getMfaBackupCodes().size());
        assertFalse(saved.getMfaBackupCodes().contains("backupCode2"));
    }

    @Test
    public void testEnableMfa() {
        when(userRepository.findOneByEmailIgnoreCase(eq("username"))).thenReturn(Optional.of(getUser("username")));
        when(encryptUtil.encrypt(Mockito.anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return "encrypted-" + (String) args[0];
            }
        });
        when(passwordEncoder.encode(Mockito.anyString())).thenAnswer(new Answer<String>() {
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
            assertThat(c.length()).isEqualTo(UserService.BACKUP_CODE_LENGTH); // check
            // code
            // has
            // not
            // been
            // hashed
        });

        verify(userRepository).save(userCaptor.capture());

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

    @Test
    void testUpdateUsersMemberName() {
        when(userRepository.updateMemberNames(eq("member-id"), eq("newName"))).thenReturn(true);

        boolean success = userService.updateUsersMemberName("member-id", "newName");
        assertThat(success).isTrue();

        verify(userRepository).updateMemberNames(eq("member-id"), eq("newName"));
    }

    private List<User> getUsersForMemberId(String memberId, int from, int to) {
        List<User> users = new ArrayList<>();
        for (int i = from; i < to; i++) {
            User user = new User();
            user.setMemberId(memberId);
            users.add(user);
        }
        return users;
    }

    private User getUserUsingMfa() {
        User user = getUser("username");
        user.setMfaEnabled(true);
        user.setId("userId");
        user.setMfaEncryptedSecret("some encrypted secret");
        user.setMfaBackupCodes(Arrays.asList("backup1", "backup2", "backupn"));
        return user;
    }

    private User getAdminUser() {
        User user = getUser("admin");
        user.setMfaEnabled(true);
        user.setId("adminId");
        user.setAdmin(true);
        user.setMfaEncryptedSecret("some encrypted secret");
        user.setMfaBackupCodes(Arrays.asList("backup1", "backup2", "backupn"));
        return user;
    }

    private User getNonAdminUser() {
        User user = getUser("nonAdmin");
        user.setMfaEnabled(true);
        user.setId("nonAdminId");
        user.setAdmin(false);
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
        user.setMemberId(login);
        return user;
    }

    private UserUpload getUserUpload() {
        UserUpload upload = new UserUpload();
        UserDTO user1 = new UserDTO();
        user1.setEmail("user1@orcid.org");
        user1.setMemberId("member-id");

        UserDTO user2 = new UserDTO();
        user2.setEmail("user2@orcid.org");
        user2.setMemberId("member-id");

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
        user.setMemberId("member");
        user.setIsAdmin(false);
        user.setAuthorities(Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));
        return user;
    }

    private Member memberWithAMEnabled() {
        Member member = new Member();
        member.setMemberId("member-id");
        member.setClientName("member");
        member.setAssertionServiceEnabled(true);
        return member;
    }

    private Member memberWithoutAMEnabled() {
        Member member = new Member();
        member.setMemberId("member-id");
        member.setClientName("member");
        member.setAssertionServiceEnabled(false);
        return member;
    }
}