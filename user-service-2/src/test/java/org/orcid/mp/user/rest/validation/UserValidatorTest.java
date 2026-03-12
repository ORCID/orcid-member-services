package org.orcid.mp.user.rest.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.user.client.MemberServiceClient;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.repository.UserRepository;
import org.orcid.mp.user.service.UserService;
import org.orcid.mp.user.validation.UserValidation;
import org.orcid.mp.user.validation.UserValidator;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class UserValidatorTest {

    @Mock
    private MessageSource messageSource;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private MemberServiceClient memberServiceClient;

    @InjectMocks
    private UserValidator userValidator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any(Locale.class))).thenReturn("error-message");
    }

    @Test
    public void testValidateUser() {
        Mockito.when(memberServiceClient.getMember(Mockito.eq("salesforce-id"))).thenReturn(getMember());

        UserDTO toValidate = getValidUser();
        UserValidation validation = userValidator.validate(toValidate, "en");
        assertThat(validation.isValid()).isTrue();
    }

    @Test
    public void testValidateUserInvalidSalesforceId() {
        Mockito.when(memberServiceClient.getMember(Mockito.eq("salesforce-id"))).thenReturn(null);

        UserDTO toValidate = getValidUser();
        UserValidation validation = userValidator.validate(toValidate, "en");
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).isNotEmpty();
        assertThat(validation.getErrors().get(0)).isEqualTo("error-message");

        Mockito.verify(messageSource, Mockito.times(1)).getMessage(Mockito.eq("user.validation.error.invalidSalesforceId"), Mockito.any(), Mockito.any());
    }

    @Test
    public void testValidateUserMissingSalesforceId() {
        Mockito.when(memberServiceClient.getMember(Mockito.eq("salesforce-id"))).thenReturn(null);

        UserDTO toValidate = getValidUser();
        toValidate.setSalesforceId(null);

        UserValidation validation = userValidator.validate(toValidate, "en");
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).isNotEmpty();
        assertThat(validation.getErrors().get(0)).isEqualTo("error-message");

        Mockito.verify(messageSource, Mockito.times(1)).getMessage(Mockito.eq("user.validation.error.missingSalesforceId"), Mockito.any(), Mockito.any());
    }

    @Test
    public void testValidateUserInvalidEmail() {
        Mockito.when(memberServiceClient.getMember(Mockito.eq("salesforce-id"))).thenReturn(null);

        UserDTO toValidate = getValidUser();
        toValidate.setEmail("invalid-email");

        UserValidation validation = userValidator.validate(toValidate, "en");
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).isNotEmpty();
        assertThat(validation.getErrors().get(0)).isEqualTo("error-message");

        Mockito.verify(messageSource, Mockito.times(1)).getMessage(Mockito.eq("user.validation.error.invalidEmail"), Mockito.any(), Mockito.any());
    }

    @Test
    public void testValidateUserMissingEmail() {
        Mockito.when(memberServiceClient.getMember(Mockito.eq("salesforce-id"))).thenReturn(null);

        UserDTO toValidate = getValidUser();
        toValidate.setEmail(null);

        UserValidation validation = userValidator.validate(toValidate, "en");
        assertThat(validation.isValid()).isFalse();
        assertThat(validation.getErrors()).isNotEmpty();
        assertThat(validation.getErrors().get(0)).isEqualTo("error-message");

        Mockito.verify(messageSource, Mockito.times(1)).getMessage(Mockito.eq("user.validation.error.missingEmail"), Mockito.any(), Mockito.any());
    }

    private UserDTO getValidUser() {
        UserDTO user = new UserDTO();
        user.setEmail("email@orcid.org");
        user.setMainContact(false);
        user.setSalesforceId("salesforce-id");
        return user;
    }

    private Member getMember() {
        Member member = new Member();
        member.setSalesforceId("salesforce-id");
        return member;
    }
}
