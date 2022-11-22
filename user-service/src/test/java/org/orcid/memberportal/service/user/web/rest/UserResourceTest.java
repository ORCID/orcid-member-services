package org.orcid.memberportal.service.user.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.user.domain.User;
import org.orcid.memberportal.service.user.dto.UserDTO;
import org.orcid.memberportal.service.user.repository.UserRepository;
import org.orcid.memberportal.service.user.services.MailService;
import org.orcid.memberportal.service.user.services.MemberService;
import org.orcid.memberportal.service.user.services.UserService;
import org.orcid.memberportal.service.user.upload.UserUpload;
import org.orcid.memberportal.service.user.validation.UserValidation;
import org.orcid.memberportal.service.user.validation.UserValidator;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

class UserResourceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private MailService mailService;

    @Mock
    private UserValidator userValidator;

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
        Mockito.when(userValidator.validate(Mockito.any(UserDTO.class), Mockito.any(User.class))).thenReturn(getUserValidation(new ArrayList<String>()));

        ResponseEntity<UserValidation> response = userResource.validateUser(new UserDTO());
        assertEquals(200, response.getStatusCodeValue());

        Mockito.verify(userValidator, Mockito.times(1)).validate(Mockito.any(UserDTO.class), Mockito.any(User.class));
    }

    @Test
    public void testValidateUser_invalidUser() throws Throwable {
        Mockito.when(userValidator.validate(Mockito.any(UserDTO.class), Mockito.any(User.class))).thenReturn(getUserValidation(Arrays.asList("some error")));

        ResponseEntity<UserValidation> response = userResource.validateUser(new UserDTO());
        assertEquals(200, response.getStatusCodeValue());

        Mockito.verify(userValidator, Mockito.times(1)).validate(Mockito.any(UserDTO.class), Mockito.any(User.class));
    }

    @Test
    public void testGetAllUsers() {
        Mockito.when(userService.getAllManagedUsers(Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(getUser(), getUser(), getUser(), getUser())));
        Mockito.when(userService.getAllManagedUsers(Mockito.any(Pageable.class), Mockito.anyString())).thenReturn(new PageImpl<>(Arrays.asList(getUser(), getUser())));

        ResponseEntity<List<UserDTO>> response = userResource.getAllUsers(new HttpHeaders(), "", UriComponentsBuilder.newInstance(), Mockito.mock(Pageable.class));
        assertNotNull(response);
        List<UserDTO> users = response.getBody();
        assertEquals(4, users.size());
        Mockito.verify(userService, Mockito.times(1)).getAllManagedUsers(Mockito.any(Pageable.class));

        response = userResource.getAllUsers(new HttpHeaders(), "some-filter", UriComponentsBuilder.newInstance(), Mockito.mock(Pageable.class));
        assertNotNull(response);
        users = response.getBody();
        assertEquals(2, users.size());
        Mockito.verify(userService, Mockito.times(1)).getAllManagedUsers(Mockito.any(Pageable.class), Mockito.anyString());
    }

    @Test
    public void testUpdateUser() {
        UserValidation userValidation = new UserValidation();
        userValidation.setValid(true);
        Mockito.when(userValidator.validate(Mockito.any(UserDTO.class), Mockito.any(User.class))).thenReturn(userValidation);
        Mockito.when(userService.updateUser(Mockito.any(UserDTO.class))).thenReturn(Optional.of(new UserDTO()));

        ResponseEntity<UserDTO> response = userResource.updateUser(new UserDTO());
        assertTrue(response.getStatusCode().is2xxSuccessful());

        Mockito.verify(userValidator, Mockito.times(1)).validate(Mockito.any(UserDTO.class), Mockito.any(User.class));
        Mockito.verify(userService, Mockito.times(1)).updateUser(Mockito.any(UserDTO.class));
    }

    @Test
    public void testGetUsersBySalesforceId() {
        Mockito.when(userService.getAllUsersBySalesforceId(Mockito.any(Pageable.class), Mockito.anyString()))
                .thenReturn(new PageImpl<>(Arrays.asList(getUser(), getUser(), getUser())));

        // same method but with filter, return page of only one user
        Mockito.when(userService.getAllUsersBySalesforceId(Mockito.any(Pageable.class), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new PageImpl<>(Arrays.asList(getUser())));

        ResponseEntity<List<UserDTO>> response = userResource.getUsersBySalesforceId("some-salesforceId", new HttpHeaders(), UriComponentsBuilder.newInstance(), "",
                Mockito.mock(Pageable.class));
        assertEquals(3, response.getBody().size());
        
        response = userResource.getUsersBySalesforceId("some-salesforceId", new HttpHeaders(), UriComponentsBuilder.newInstance(), "some filter",
                Mockito.mock(Pageable.class));
        assertEquals(1, response.getBody().size());
    }

    private UserDTO getUser() {
        UserDTO user = new UserDTO();
        return user;
    }

    private Optional<User> getCurrentUser() {
        User user = new User();
        user.setEmail("some@email.com");
        user.setLangKey("en");
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
