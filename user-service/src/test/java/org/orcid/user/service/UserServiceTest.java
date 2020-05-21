package org.orcid.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
	void testCreateUser() {
		Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(new Answer<User>() {
			@Override
			public User answer(InvocationOnMock invocation) throws Throwable {
				return (User) invocation.getArgument(0);
			}
		});
		Mockito.doNothing().when(mailService).sendCreationEmail(Mockito.any(User.class));
		
		UserDTO userDTO = getUserDTO();
		userService.createUser(userDTO);
		
		Mockito.verify(userRepository, Mockito.times(1)).save(userCaptor.capture());
		Mockito.verify(mailService, Mockito.times(1)).sendCreationEmail(Mockito.any(User.class));
		
		User user = userCaptor.getValue();
		assertEquals(userDTO.getFirstName(), user.getFirstName());
		assertEquals(userDTO.getLastName(), user.getLastName());
		assertEquals(userDTO.getLogin(), user.getLogin());
		assertEquals(userDTO.getEmail(), user.getEmail());
		assertEquals(userDTO.getAuthorities(), user.getAuthorities());
	}

	private UserDTO getUserDTO() {
		UserDTO user = new UserDTO();
		user.setActivated(false);
		user.setAssertionServiceEnabled(true);
		user.setAuthorities(Stream.of(AuthoritiesConstants.USER, AuthoritiesConstants.ASSERTION_SERVICE_ENABLED)
				.collect(Collectors.toSet()));
		user.setEmail("email@email.com");
		user.setFirstName("first");
		user.setLastName("last");
		user.setLogin("email@email.com");
		user.setSalesforceId("member");
		return user;

	}

}
