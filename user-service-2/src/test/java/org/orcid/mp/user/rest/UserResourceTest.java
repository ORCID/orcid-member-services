package org.orcid.mp.user.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.mapper.UserMapper;
import org.orcid.mp.user.repository.UserRepository;
import org.orcid.mp.user.validation.UserValidation;
import org.orcid.mp.user.validation.UserValidator;
import org.orcid.mp.user.service.UserService;
import org.orcid.mp.user.upload.UserUpload;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Authentication authentication = Mockito.mock(Authentication.class);
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        Mockito.when(authentication.getPrincipal()).thenReturn("some@email.com");
        Mockito.when(userRepository.findOneByEmailIgnoreCase(Mockito.eq("some@email.com"))).thenReturn(getCurrentUser());

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testUpdateSalesforceId() {
        Mockito.when(userService.updateUsersSalesforceId(Mockito.eq("salesforce-id"), Mockito.eq("new-salesforce-id"))).thenReturn(true);
        ResponseEntity<Void> response = userResource.updateUsersSalesforceId("salesforce-id", "new-salesforce-id");
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testUpdateUsersMemberName() {
        Mockito.when(userService.updateUsersMemberName(Mockito.eq("salesforce-id"), Mockito.eq("newName"))).thenReturn(true);
        ResponseEntity<Void> response = userResource.updateUsersMemberName("salesforce-id", "newName");
        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void testUpdateSalesforceIdWithError() {
        Mockito.when(userService.updateUsersSalesforceId(Mockito.eq("salesforce-id"), Mockito.eq("new-salesforce-id"))).thenReturn(false);
        ResponseEntity<Void> response = userResource.updateUsersSalesforceId("salesforce-id", "new-salesforce-id");
        assertTrue(response.getStatusCode().is5xxServerError());
    }

    @Test
    public void testResendActivation() {
        Mockito.doNothing().when(userService).resendActivationEmail(Mockito.anyString());
        userResource.resendActivation("key");
        Mockito.verify(userService, Mockito.times(1)).resendActivationEmail(Mockito.eq("key"));
    }

    @Test
    public void testUploadUsers() throws Throwable {
        Mockito.when(userService.uploadUserCSV(Mockito.any(InputStream.class), Mockito.any(User.class))).thenReturn(getUserUpload());
        MultipartFile file = Mockito.mock(MultipartFile.class);
        InputStream inputStream = Mockito.mock(InputStream.class);
        Mockito.when(file.getInputStream()).thenReturn(inputStream);

        userResource.uploadUsers(file);

        Mockito.verify(userService, Mockito.times(1)).uploadUserCSV(Mockito.any(InputStream.class), Mockito.any(User.class));
    }

    @Test
    public void testValidateUser_validUser() throws Throwable {
        Mockito.when(userValidator.validate(Mockito.any(UserDTO.class), Mockito.anyString())).thenReturn(getUserValidation(new ArrayList<String>()));

        ResponseEntity<UserValidation> response = userResource.validateUser(new UserDTO());
        assertEquals(200, response.getStatusCodeValue());

        Mockito.verify(userValidator, Mockito.times(1)).validate(Mockito.any(UserDTO.class), Mockito.anyString());
    }

    @Test
    public void testValidateUser_invalidUser() throws Throwable {
        Mockito.when(userValidator.validate(Mockito.any(UserDTO.class), Mockito.anyString())).thenReturn(getUserValidation(Arrays.asList("some error")));

        ResponseEntity<UserValidation> response = userResource.validateUser(new UserDTO());
        assertEquals(200, response.getStatusCodeValue());

        Mockito.verify(userValidator, Mockito.times(1)).validate(Mockito.any(UserDTO.class), Mockito.anyString());
    }

    @Test
    public void testGetAllUsers() { /*
        Mockito.when(userService.getAllManagedUsers(Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(getUser(), getUser(), getUser(), getUser())));
        Mockito.when(userService.getAllManagedUsers(Mockito.any(Pageable.class), Mockito.anyString())).thenReturn(new PageImpl<>(Arrays.asList(getUser(), getUser())));

        ResponseEntity<Paged<UserDTO>> response = userResource.getAllUsers("", Mockito.mock(Pageable.class));
        assertNotNull(response);
        Page<UserDTO> users = response.getBody();
        assertEquals(4, users.getTotalElements());
        Mockito.verify(userService, Mockito.times(1)).getAllManagedUsers(Mockito.any(Pageable.class));

        response = userResource.getAllUsers("some-filter", Mockito.mock(Pageable.class));
        assertNotNull(response);
        users = response.getBody();
        assertEquals(2, users.getTotalElements());
        Mockito.verify(userService, Mockito.times(1)).getAllManagedUsers(Mockito.any(Pageable.class), Mockito.anyString());  */
    }

    @Test
    public void testUpdateUser() {
        UserValidation userValidation = new UserValidation();
        userValidation.setValid(true);
        Mockito.when(userValidator.validate(Mockito.any(UserDTO.class), Mockito.anyString())).thenReturn(userValidation);
        Mockito.when(userService.updateUser(Mockito.any(UserDTO.class))).thenReturn(Optional.of(new UserDTO()));

        ResponseEntity<UserDTO> response = userResource.updateUser(new UserDTO());
        assertTrue(response.getStatusCode().is2xxSuccessful());

        Mockito.verify(userValidator, Mockito.times(1)).validate(Mockito.any(UserDTO.class), Mockito.anyString());
        Mockito.verify(userService, Mockito.times(1)).updateUser(Mockito.any(UserDTO.class));
    }

    @Test
    public void testGetUsersBySalesforceId() { /*
        Mockito.when(userService.getAllUsersBySalesforceId(Mockito.any(Pageable.class), Mockito.anyString()))
                .thenReturn(new PageImpl<>(Arrays.asList(getUser(), getUser(), getUser())));

        // same method but with filter, return page of only one user
        Mockito.when(userService.getAllUsersBySalesforceId(Mockito.any(Pageable.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new PageImpl<>(Arrays.asList(getUser())));

        ResponseEntity<Page<UserDTO>> response = userResource.getUsersBySalesforceId("some-salesforceId", new HttpHeaders(), UriComponentsBuilder.newInstance(), "",
                Mockito.mock(Pageable.class));
        assertEquals(3, response.getBody().getTotalElements());

        response = userResource.getUsersBySalesforceId("some-salesforceId", new HttpHeaders(), UriComponentsBuilder.newInstance(), "some filter",
                Mockito.mock(Pageable.class));
        assertEquals(1, response.getBody().getTotalElements()); */
    }

    private UserDTO getUser() {
        UserDTO user = new UserDTO();
        return user;
    }

    private Optional<User> getCurrentUser() {
        User user = new User();
        user.setEmail("some@email.com");
        user.setLangKey("en");
        user.setSalesforceId("some-salesforceId");
        user.setMainContact(true);
        return Optional.of(user);
    }

    private UserValidation getUserValidation(List<String> errors) {
        UserValidation validation = new UserValidation();
        validation.setValid(errors.isEmpty());
        validation.setErrors(errors);
        return validation;
    }

    private UserUpload getUserUpload() {
        UserUpload upload = new UserUpload();
        return upload;
    }

}