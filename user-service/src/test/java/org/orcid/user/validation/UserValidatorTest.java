package org.orcid.user.validation;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.user.domain.User;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.service.UserService;
import org.orcid.user.service.dto.UserDTO;
import org.springframework.context.MessageSource;

import static org.assertj.core.api.Assertions.assertThat;

public class UserValidatorTest {

    @Mock
    private MessageSource messageSource;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserValidator userValidator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any(Locale.class)))
                .thenReturn("error-message");
    }

    @Test
    public void testValidateUser() {
        Mockito.when(userService.memberExists(Mockito.eq("salesforce-id"))).thenReturn(true);

        UserDTO toValidate = getValidUser();
        UserValidation validation = userValidator.validate(toValidate, getUser());
        assertThat(validation.isValid()).isTrue();
    }

    @Test
    public void testValidateUserInvalidSalesforceId() {
        Mockito.when(userService.memberExists(Mockito.eq("salesforce-id"))).thenReturn(false);

        UserDTO toValidate = getValidUser();
        UserValidation validation = userValidator.validate(toValidate, getUser());
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).isNotEmpty();
        assertThat(validation.getErrors().get(0)).isEqualTo("error-message");

        Mockito.verify(messageSource, Mockito.times(1))
                .getMessage(Mockito.eq("user.validation.error.invalidSalesforceId"), Mockito.any(), Mockito.any());
    }

    @Test
    public void testValidateUserMissingSalesforceId() {
        Mockito.when(userService.memberExists(Mockito.eq("salesforce-id"))).thenReturn(false);

        UserDTO toValidate = getValidUser();
        toValidate.setSalesforceId(null);

        UserValidation validation = userValidator.validate(toValidate, getUser());
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).isNotEmpty();
        assertThat(validation.getErrors().get(0)).isEqualTo("error-message");

        Mockito.verify(messageSource, Mockito.times(1))
                .getMessage(Mockito.eq("user.validation.error.missingSalesforceId"), Mockito.any(), Mockito.any());
    }

    @Test
    public void testValidateUserInvalidEmail() {
        Mockito.when(userService.memberExists(Mockito.eq("salesforce-id"))).thenReturn(false);

        UserDTO toValidate = getValidUser();
        toValidate.setEmail("invalid-email");

        UserValidation validation = userValidator.validate(toValidate, getUser());
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).isNotEmpty();
        assertThat(validation.getErrors().get(0)).isEqualTo("error-message");

        Mockito.verify(messageSource, Mockito.times(1)).getMessage(Mockito.eq("user.validation.error.invalidEmail"),
                Mockito.any(), Mockito.any());
    }

    @Test
    public void testValidateUserMissingEmail() {
        Mockito.when(userService.memberExists(Mockito.eq("salesforce-id"))).thenReturn(false);

        UserDTO toValidate = getValidUser();
        toValidate.setEmail(null);

        UserValidation validation = userValidator.validate(toValidate, getUser());
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).isNotEmpty();
        assertThat(validation.getErrors().get(0)).isEqualTo("error-message");

        Mockito.verify(messageSource, Mockito.times(1)).getMessage(Mockito.eq("user.validation.error.missingEmail"),
                Mockito.any(), Mockito.any());
    }

    @Test
    public void testValidateUserChangeMainContact() {
        Mockito.when(userService.memberExists(Mockito.eq("salesforce-id"))).thenReturn(false);
        Mockito.when(userRepository.findOneBySalesforceIdAndMainContactIsTrue(Mockito.eq("salesforce-id")))
                .thenReturn(Optional.of(getUser()));

        UserDTO toValidate = getValidUser();
        toValidate.setMainContact(true);
    }

    private User getUser() {
        User user = new User();
        user.setLangKey("en");
        user.setEmail("some-email@orcid.org");
        return user;
    }

    private UserDTO getValidUser() {
        UserDTO user = new UserDTO();
        user.setEmail("email@orcid.org");
        user.setMainContact(false);
        user.setSalesforceId("salesforce-id");
        return user;
    }

}
