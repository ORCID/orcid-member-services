package org.orcid.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
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
import org.orcid.user.service.cache.UserCaches;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.upload.UserUpload;
import org.orcid.user.upload.UserUploadReader;
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

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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
        userToBeCleared.setLogin("something@somewhere.com");

        Mockito.when(userRepository.findOneById(Mockito.eq("some-id"))).thenReturn(Optional.of(userToBeCleared));
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenReturn(null);

        userService.clearUser("some-id");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        Mockito.verify(userRepository).save(captor.capture());
        User cleared = captor.getValue();
        assertNull(cleared.getFirstName());
        assertNull(cleared.getLastName());
        assertEquals("some-id@deleted.orcid.org", cleared.getEmail());
        assertEquals("some-id@deleted.orcid.org", cleared.getLogin());
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
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString()))
                .thenReturn(true);

        UserDTO userDTO = getUserDTO();
        userService.createUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(userCaptor.capture());
        Mockito.verify(mailService, Mockito.times(1)).sendCreationEmail(Mockito.any(User.class));
        Mockito.verify(memberService, Mockito.times(1))
                .memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getLogin(), user.getLogin());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertTrue(user.getAuthorities().contains(AuthoritiesConstants.USER));
        assertTrue(user.getAuthorities().contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED));
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
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString()))
                .thenReturn(false);

        UserDTO userDTO = getUserDTO();
        userService.createUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(userCaptor.capture());
        Mockito.verify(mailService, Mockito.times(1)).sendCreationEmail(Mockito.any(User.class));
        Mockito.verify(memberService, Mockito.times(1))
                .memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getLogin(), user.getLogin());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertTrue(user.getAuthorities().contains(AuthoritiesConstants.USER));
        assertFalse(user.getAuthorities().contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED));
    }

    @Test
    void testUpdateUserForMemberWithAsserionsEnabled() {
        Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
            @Override
            public User answer(InvocationOnMock invocation) throws Throwable {
                return (User) invocation.getArgument(0);
            }
        });
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString()))
                .thenReturn(true);
        Mockito.when(userRepository.findById(Mockito.anyString())).thenReturn(Optional.of(new User()));

        UserDTO userDTO = getUserDTO();
        userDTO.setId("id");
        userService.updateUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(userCaptor.capture());
        Mockito.verify(memberService, Mockito.times(1))
                .memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getLogin(), user.getLogin());
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
        Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString()))
                .thenReturn(false);
        Mockito.when(userRepository.findById(Mockito.anyString())).thenReturn(Optional.of(new User()));

        UserDTO userDTO = getUserDTO();
        userDTO.setId("id");
        userService.updateUser(userDTO);

        Mockito.verify(userRepository, Mockito.times(1)).save(userCaptor.capture());
        Mockito.verify(memberService, Mockito.times(1))
                .memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

        User user = userCaptor.getValue();
        assertEquals(userDTO.getFirstName(), user.getFirstName());
        assertEquals(userDTO.getLastName(), user.getLastName());
        assertEquals(userDTO.getLogin(), user.getLogin());
        assertEquals(userDTO.getEmail(), user.getEmail());
        assertTrue(user.getAuthorities().contains(AuthoritiesConstants.USER));
        assertFalse(user.getAuthorities().contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED));
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
        Mockito.when(userRepository.findOneByLogin(Mockito.anyString())).thenReturn(Optional.of(user));

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
        assertTrue(
                AuthoritiesConstants.ADMIN.equals(authorities[0]) || AuthoritiesConstants.ADMIN.equals(authorities[1]));
        assertTrue(
                AuthoritiesConstants.USER.equals(authorities[0]) || AuthoritiesConstants.USER.equals(authorities[1]));
    }

    @Test
    public void testUploadUserCSV() throws IOException {
        Mockito.when(usersUploadReader.readUsersUpload(Mockito.any(InputStream.class), Mockito.any(User.class)))
                .thenReturn(getUserUpload());
        Mockito.when(userRepository.findOneByLogin(Mockito.eq("user1@orcid.org"))).thenReturn(Optional.empty());
        Mockito.when(userRepository.findOneByLogin(Mockito.eq("user2@orcid.org")))
                .thenReturn(Optional.of(getUser("user2@orcid.org")));
        Mockito.when(userRepository.findOneById(Mockito.eq("user2@orcid.org")))
                .thenReturn(Optional.of(getUser("user2@orcid.org")));

        InputStream inputStream = Mockito.mock(InputStream.class);
        userService.uploadUserCSV(inputStream, getUser("some-user@orcid.org"));

        // check only new users saved
        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(User.class));
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

    private User getUser(String login) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(login);
        user.setId(login);
        return user;
    }

    private UserUpload getUserUpload() {
        UserUpload upload = new UserUpload();
        UserDTO user1 = new UserDTO();
        user1.setLogin("user1@orcid.org");
        user1.setEmail("user1@orcid.org");

        UserDTO user2 = new UserDTO();
        user2.setLogin("user2@orcid.org");
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
        user.setLogin("email@email.com");
        user.setSalesforceId("member");
        user.setIsAdmin(false);
        user.setAuthorities(Stream.of(AuthoritiesConstants.USER).collect(Collectors.toSet()));
        return user;
    }

}
