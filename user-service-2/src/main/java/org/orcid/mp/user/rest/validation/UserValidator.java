package org.orcid.mp.user.rest.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.orcid.mp.user.client.MemberServiceClient;
import org.orcid.mp.user.domain.Member;
import org.orcid.mp.user.domain.User;
import org.orcid.mp.user.dto.UserDTO;
import org.orcid.mp.user.repository.UserRepository;
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
    private MemberServiceClient memberServiceClient;

    private EmailValidator emailValidator = EmailValidator.getInstance(false);

    public UserValidation validate(UserDTO user, String langKey) {
        List<String> errors = new ArrayList<>();
        validateEmail(user, langKey, errors);
        validateSalesforceId(user, langKey, errors);

        UserValidation validation = new UserValidation();
        validation.setValid(errors.isEmpty());
        validation.setErrors(errors);
        return validation;
    }

    private void validateSalesforceId(UserDTO user, String langKey, List<String> errors) {
        Member member = memberServiceClient.getMember(user.getSalesforceId());
        String salesforceId = user.getSalesforceId();
        if (StringUtils.isBlank(salesforceId)) {
            errors.add(getError("missingSalesforceId", langKey));
        } else if (member == null) {
            errors.add(getError("invalidSalesforceId", salesforceId, langKey));
        } else if (user.getIsAdmin() == true && user.getSalesforceId() != null && !member.getSuperadminEnabled()) {
            errors.add(getError("superAdminNotAllowed", langKey));
        }
    }

    private void validateEmail(UserDTO user, String langKey, List<String> errors) {
        String email = user.getEmail();
        if (StringUtils.isBlank(email)) {
            errors.add(getError("missingEmail", langKey));
        } else if (!emailValidator.isValid(email)) {
            errors.add(getError("invalidEmail", email, langKey));
        } else {
            if (StringUtils.isBlank(user.getId()) && userExists(email)) {
                errors.add(getError("userExists", email, langKey));
            }
        }
    }

    private Boolean userExists(String email) {
        Optional<User> existingUser = userRepository.findOneByEmailIgnoreCase(email);
        return existingUser.isPresent();
    }

    private String getError(String code, String langKey) {
        return getError(code, null, langKey);
    }

    private String getError(String code, String arg, String langKey) {
        return messageSource.getMessage("user.validation.error." + code, arg != null ? new Object[] { arg } : null, Locale.forLanguageTag(langKey));
    }

}
