package org.orcid.mp.user.rest;

// 1. JUnit 5 Imports
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 2. Mockito Imports
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 3. Spring Framework & Security Imports
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

// 4. Your Project Domain/Service Imports
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.error.BadRequestAlertException;
import org.orcid.mp.user.mapper.UserMapper;
import org.orcid.mp.user.pojo.ResendActivationResponse;
import org.orcid.mp.user.repository.UserRepository;
import org.orcid.mp.user.service.UserService;
import org.orcid.mp.user.validation.UserValidation;
import org.orcid.mp.user.validation.UserValidator;

// 5. Standard Java Imports
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserResourceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserValidator userValidator;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserResource userResource;

    private static final String CURRENT_USER_EMAIL = "test@email.com";
    private static final String CURRENT_MEMBER_ID = "some-memberId";

    @BeforeEach
    public void setUp() {
        // 1. Mock Security Context
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        // Use lenient() so Mockito doesn't throw UnnecessaryStubbingException on tests that don't need the principal
        lenient().when(authentication.getPrincipal()).thenReturn(CURRENT_USER_EMAIL);

        // 2. Mock the repository call that userResource.getCurrentUser() relies on
        lenient().when(userRepository.findOneByEmailIgnoreCase(CURRENT_USER_EMAIL))
                .thenReturn(Optional.of(getMockUser()));

        // 3. Mock the Request Context for URI building
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    public void testUpdateUser() {
        UserDTO inputDto = getMockUserDTO();
        when(userService.updateUser(any(UserDTO.class), eq("en"))).thenReturn(Optional.of(inputDto));

        ResponseEntity<UserDTO> response = userResource.updateUser(inputDto);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        verify(userService, times(1)).updateUser(inputDto, "en");
    }

    @Test
    public void testGetAllUsers() {
        Page<UserDTO> mockPage = new PageImpl<>(List.of(getMockUserDTO(), getMockUserDTO()));

        // Test without filter
        when(userService.getAllManagedUsers(any(Pageable.class))).thenReturn(mockPage);
        ResponseEntity<Page<UserDTO>> response = userResource.getAllUsers(null, Pageable.unpaged());

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(2, response.getBody().getTotalElements());
        verify(userService, times(1)).getAllManagedUsers(any(Pageable.class));

        // Test with filter
        when(userService.getAllManagedUsers(any(Pageable.class), eq("active"))).thenReturn(mockPage);
        ResponseEntity<Page<UserDTO>> filteredResponse = userResource.getAllUsers("active", Pageable.unpaged());

        assertTrue(filteredResponse.getStatusCode().is2xxSuccessful());
        assertEquals(2, filteredResponse.getBody().getTotalElements());
        verify(userService, times(1)).getAllManagedUsers(any(Pageable.class), eq("active"));
    }

    @Test
    public void testGetUsersByMemberId_List() {
        when(userService.getAllUsersByMemberId(CURRENT_MEMBER_ID)).thenReturn(List.of(getMockUserDTO()));

        ResponseEntity<List<UserDTO>> response = userResource.getUsersByMemberId(CURRENT_MEMBER_ID);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(1, response.getBody().size());
    }

    @Test
    public void testGetUsersByMemberId_List_ThrowsErrorOnMismatch() {
        assertThrows(BadRequestAlertException.class, () -> {
            userResource.getUsersByMemberId("wrong-member-id");
        });
    }

    @Test
    public void testGetUsersByMemberId_Paged() {
        Page<UserDTO> mockPage = new PageImpl<>(List.of(getMockUserDTO()));
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance();

        // Test without filter
        when(userService.getAllUsersByMemberId(any(Pageable.class), eq(CURRENT_MEMBER_ID))).thenReturn(mockPage);

        ResponseEntity<Page<UserDTO>> response = userResource.getUsersByMemberId(
                CURRENT_MEMBER_ID, new LinkedMultiValueMap<>(), uriBuilder, null, Pageable.unpaged());

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(1, response.getBody().getTotalElements());

        // Test with filter
        when(userService.getAllUsersByMemberId(any(Pageable.class), eq(CURRENT_MEMBER_ID), eq("filterText"))).thenReturn(mockPage);

        ResponseEntity<Page<UserDTO>> filteredResponse = userResource.getUsersByMemberId(
                CURRENT_MEMBER_ID, new LinkedMultiValueMap<>(), uriBuilder, "filterText", Pageable.unpaged());

        assertTrue(filteredResponse.getStatusCode().is2xxSuccessful());
    }

    @Test
    public void testGetUserByLogin() {
        User mockUser = getMockUser();
        when(userService.getUserByLogin("someLogin")).thenReturn(Optional.of(mockUser));
        when(userMapper.toUserDTO(mockUser)).thenReturn(getMockUserDTO());

        ResponseEntity<UserDTO> response = userResource.getUserByLogin("someLogin");

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
    }

    @Test
    public void testCreateUser() throws URISyntaxException {
        UserDTO dtoToSave = getMockUserDTO();
        when(userService.createUser(any(UserDTO.class), eq(CURRENT_USER_EMAIL), eq("en"))).thenReturn(dtoToSave);

        ResponseEntity<UserDTO> response = userResource.createUser(dtoToSave);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        verify(userService, times(1)).createUser(any(UserDTO.class), eq(CURRENT_USER_EMAIL), eq("en"));
    }

    @Test
    public void testValidateUser() throws URISyntaxException {
        UserValidation validResponse = new UserValidation();
        validResponse.setValid(true);
        validResponse.setErrors(Collections.emptyList());

        when(userValidator.validate(any(UserDTO.class), eq("en"))).thenReturn(validResponse);

        ResponseEntity<UserValidation> response = userResource.validateUser(getMockUserDTO());

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody().isValid());
    }

    @Test
    public void testDeleteUser_Success() {
        User targetUser = new User();
        targetUser.setEmail("target@email.com");
        targetUser.setMainContact(false);
        targetUser.setAdmin(false);

        when(userService.getUser("targetId")).thenReturn(Optional.of(targetUser));

        ResponseEntity<Void> response = userResource.deleteUser("targetId", false);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(userService, times(1)).clearUser("targetId");
    }

    @Test
    public void testDeleteUser_CannotDeleteSelf() {
        assertThrows(BadRequestAlertException.class, () -> {
            userResource.deleteUser(CURRENT_USER_EMAIL, false);
        });
    }

    @Test
    public void testSendActivate() {
        User mockUser = getMockUser();
        when(userService.getUserByLogin("userToActivate")).thenReturn(Optional.of(mockUser));
        when(userMapper.toUserDTO(mockUser)).thenReturn(getMockUserDTO());

        ResponseEntity<UserDTO> response = userResource.sendActivate("userToActivate");

        assertTrue(response.getStatusCode().is2xxSuccessful());
        verify(userService, times(1)).sendActivationEmail(mockUser.getEmail());
    }

    @Test
    public void testResendActivation_Success() {
        doNothing().when(userService).resendActivationEmail("validKey");

        ResponseEntity<ResendActivationResponse> response = userResource.resendActivation("validKey");

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody().isResent());
        verify(userService, times(1)).resendActivationEmail("validKey");
    }

    @Test
    public void testResendActivation_Failure() {
        doThrow(new RuntimeException("Expired")).when(userService).resendActivationEmail("invalidKey");

        ResponseEntity<ResendActivationResponse> response = userResource.resendActivation("invalidKey");

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertFalse(response.getBody().isResent());
    }

    @Test
    public void testGetOwner() {
        when(userService.hasOwnerForMemberId(CURRENT_MEMBER_ID)).thenReturn(true);

        boolean isOwner = userResource.getOwner(CURRENT_MEMBER_ID);

        assertTrue(isOwner);
        verify(userService, times(1)).hasOwnerForMemberId(CURRENT_MEMBER_ID);
    }

    // --- Helper Methods to generate mock data ---

    private User getMockUser() {
        User user = new User();
        user.setEmail(CURRENT_USER_EMAIL);
        user.setLangKey("en");
        user.setMemberId(CURRENT_MEMBER_ID);
        user.setMainContact(true);
        user.setAdmin(false);
        return user;
    }

    private UserDTO getMockUserDTO() {
        UserDTO dto = new UserDTO();
        dto.setEmail("dto@email.com");
        dto.setMemberId(CURRENT_MEMBER_ID);
        return dto;
    }
}