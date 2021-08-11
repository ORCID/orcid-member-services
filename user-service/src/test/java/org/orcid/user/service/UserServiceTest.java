package org.orcid.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.orcid.user.domain.User;
import org.orcid.user.repository.AuthorityRepository;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.security.AuthoritiesConstants;
import org.orcid.user.security.MockSecurityContext;
import org.orcid.user.service.cache.UserCaches;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.service.mapper.UserMapper;
import org.orcid.user.upload.UserUpload;
import org.orcid.user.upload.UserUploadReader;
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
    private UserCaches userCaches;

    @Mock
    private UserUploadReader usersUploadReader;

    @Mock
    private MemberService memberService;

    @Mock
    private MailService mailService;

    @Mock
    private UserMapper userMapper;

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
        Mockito.doNothing().when(mailService).sendCreationEmail(Mockito.any(User.class));
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString())).thenReturn(true);
        Mockito.when(userMapper.toUser(Mockito.any(UserDTO.class))).thenReturn(new User());

        UserDTO userDTO = getUserDTO();
        userService.createUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        Mockito.verify(mailService, Mockito.times(1)).sendCreationEmail(Mockito.any(User.class));
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
        Mockito.doNothing().when(mailService).sendCreationEmail(Mockito.any(User.class));
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString())).thenReturn(false);
        Mockito.when(userMapper.toUser(Mockito.any(UserDTO.class))).thenReturn(new User());

        UserDTO userDTO = getUserDTO();
        userService.createUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
        Mockito.verify(mailService, Mockito.times(1)).sendCreationEmail(Mockito.any(User.class));
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
        existing.setEmail("email@orcid.orgd");
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
        Mockito.doNothing().when(mailService).sendCreationEmail(Mockito.any(User.class));
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
