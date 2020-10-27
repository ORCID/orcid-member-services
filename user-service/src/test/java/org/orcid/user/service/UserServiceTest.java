package org.orcid.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
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
import org.orcid.user.upload.UserUploadReader;
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
	void testCreateUserForMemberWithAsserionsEnabled() {
		Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
			@Override
			public User answer(InvocationOnMock invocation) throws Throwable {
				return (User) invocation.getArgument(0);
			}
		});
		Mockito.doNothing().when(mailService).sendCreationEmail(Mockito.any(User.class));
		Mockito.when(memberService.memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString())).thenReturn(true);

		UserDTO userDTO = getUserDTO();
		userService.createUser(userDTO);

		Mockito.verify(userRepository, Mockito.times(1)).save(userCaptor.capture());
		Mockito.verify(mailService, Mockito.times(1)).sendCreationEmail(Mockito.any(User.class));
		Mockito.verify(memberService, Mockito.times(1)).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

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
		Mockito.verify(memberService, Mockito.times(1)).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

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
		Mockito.verify(memberService, Mockito.times(1)).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

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
		Mockito.verify(memberService, Mockito.times(1)).memberExistsWithSalesforceIdAndAssertionsEnabled(Mockito.anyString());

		User user = userCaptor.getValue();
		assertEquals(userDTO.getFirstName(), user.getFirstName());
		assertEquals(userDTO.getLastName(), user.getLastName());
		assertEquals(userDTO.getLogin(), user.getLogin());
		assertEquals(userDTO.getEmail(), user.getEmail());
		assertTrue(user.getAuthorities().contains(AuthoritiesConstants.USER));
		assertFalse(user.getAuthorities().contains(AuthoritiesConstants.ASSERTION_SERVICE_ENABLED));
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
