package org.orcid.user.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.orcid.user.domain.User;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.service.MailService;
import org.orcid.user.service.MemberService;
import org.orcid.user.service.UserService;
import org.orcid.user.service.dto.UserDTO;
import org.orcid.user.upload.UserUpload;
import org.orcid.user.validation.UserValidation;
import org.orcid.user.validation.UserValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

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
        Mockito.when(userRepository.findOneByLogin(Mockito.eq("some@email.com"))).thenReturn(getCurrentUser());
    }

    @Test
    public void testResendActivate() {
        Mockito.doNothing().when(userService).resendActivationEmail(Mockito.anyString());
        userResource.resendActivate("key");
        Mockito.verify(userService, Mockito.times(1)).resendActivationEmail(Mockito.eq("key"));
    }
    
    @Test
    public void testUploadUsers() throws Throwable {
        Mockito.when(userService.uploadUserCSV(Mockito.any(InputStream.class), Mockito.any(User.class)))
                .thenReturn(getUserUpload());
        MultipartFile file = Mockito.mock(MultipartFile.class);
        InputStream inputStream = Mockito.mock(InputStream.class);
        Mockito.when(file.getInputStream()).thenReturn(inputStream);

        userResource.uploadUsers(file);

        Mockito.verify(userService, Mockito.times(1)).uploadUserCSV(Mockito.any(InputStream.class),
                Mockito.any(User.class));
    }

    @Test
    public void testValidateUser_validUser() throws Throwable {
        Mockito.when(userValidator.validate(Mockito.any(UserDTO.class), Mockito.any(User.class)))
                .thenReturn(getUserValidation(new ArrayList<String>()));

        ResponseEntity<UserValidation> response = userResource.validateUser(new UserDTO());
        assertEquals(200, response.getStatusCodeValue());

        Mockito.verify(userValidator, Mockito.times(1)).validate(Mockito.any(UserDTO.class), Mockito.any(User.class));
    }

    @Test
    public void testValidateUser_invalidUser() throws Throwable {
        Mockito.when(userValidator.validate(Mockito.any(UserDTO.class), Mockito.any(User.class)))
                .thenReturn(getUserValidation(Arrays.asList("some error")));

        ResponseEntity<UserValidation> response = userResource.validateUser(new UserDTO());
        assertEquals(200, response.getStatusCodeValue());

        Mockito.verify(userValidator, Mockito.times(1)).validate(Mockito.any(UserDTO.class), Mockito.any(User.class));
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
