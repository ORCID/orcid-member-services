package org.orcid.user.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.orcid.user.domain.User;
import org.orcid.user.repository.UserRepository;
import org.orcid.user.service.UserService;
import org.orcid.user.service.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class UserValidator {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private EmailValidator emailValidator = EmailValidator.getInstance(false);

    public UserValidation validate(UserDTO user, User currentUser) {
        List<String> errors = new ArrayList<>();
        validateEmail(user, currentUser, errors);
        validateSalesforceId(user, currentUser, errors);

        UserValidation validation = new UserValidation();
        validation.setValid(errors.isEmpty());
        validation.setErrors(errors);
        return validation;
    }

    private void validateSalesforceId(UserDTO user, User currentUser, List<String> errors) {
        String salesforceId = user.getSalesforceId();
        if (StringUtils.isBlank(salesforceId)) {
            errors.add(getError("missingSalesforceId", currentUser));
        } else if (!userService.memberExists(salesforceId)) {
            errors.add(getError("invalidSalesforceId", salesforceId, currentUser));
        } else if (user.getIsAdmin() == true && user.getSalesforceId() != null && !userService.memberSuperadminEnabled(user.getSalesforceId())) {
            errors.add(getError("superAdminNotAllowed", currentUser));
        }
    }

    private void validateEmail(UserDTO user, User currentUser, List<String> errors) {
        String email = user.getEmail();
        if (StringUtils.isBlank(email)) {
            errors.add(getError("missingEmail", currentUser));
        } else if (!emailValidator.isValid(email)) {
            errors.add(getError("invalidEmail", email, currentUser));
        } else {
            if (StringUtils.isBlank(user.getId()) && userExists(email)) {
                errors.add(getError("userExists", email, currentUser));
            }
        }
    }

    private Boolean userExists(String email) {
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(email);
        return existingUser.isPresent();
    }

    private String getError(String code, User user) {
        return getError(code, null, user);
    }

    private String getError(String code, String arg, User user) {
        return messageSource.getMessage("user.validation.error." + code, arg != null ? new Object[] { arg } : null, Locale.forLanguageTag(user.getLangKey()));
    }

}
