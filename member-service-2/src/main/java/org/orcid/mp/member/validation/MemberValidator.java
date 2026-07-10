package org.orcid.mp.member.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.micrometer.common.util.StringUtils;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class MemberValidator {

    private static final String OLD_CLIENT_ID_REGEX = "[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$";
    private static final String NEW_CLIENT_ID_PREFIX = "APP-";
    private static final String NEW_CLIENT_ID_REGEX = NEW_CLIENT_ID_PREFIX + "[A-Z0-9]{16}$";
    private static final Pattern OLD_CLIENT_ID_PATTERN = Pattern.compile(OLD_CLIENT_ID_REGEX);
    private static final Pattern NEW_CLIENT_ID_PATTERN = Pattern.compile(NEW_CLIENT_ID_REGEX);
    private static final String MEMBER_TYPE_BASIC = "basic";
    private static final String MEMBER_TYPE_PREMIUM = "premium";

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private MemberRepository memberRepository;

    public MemberValidation validate(Member member, String langKey) {
        List<String> errors = new ArrayList<>();
        validateAssertionServiceEnabled(member, langKey, errors);
        validateSalesforceId(member, langKey, errors);
        validateConsortiumLeadAndParentSalesforceId(member, langKey, errors);
        validateClientId(member, langKey, errors);
        validateClientName(member, langKey, errors);
        validateType(member, langKey, errors);

        MemberValidation validation = new MemberValidation();
        validation.setValid(errors.isEmpty());
        validation.setErrors(errors);
        return validation;
    }

    private void validateClientName(Member member, String langKey, List<String> errors) {
        if (StringUtils.isBlank(member.getClientName())) {
            errors.add(getError("missingClientName", langKey));
        } else {
            Optional<Member> matchingName = memberRepository.findByClientName(member.getClientName());
            if (matchingName.isPresent() && !matchingName.get().getId().equals(member.getId())) {
                errors.add(getError("nameAlreadyExists", member.getClientName(), langKey));
            }
        }
    }

    private void validateClientId(Member member, String langKey, List<String> errors) {
        if (Boolean.FALSE.equals(member.getIsConsortiumLead()) && Boolean.TRUE.equals(member.getAssertionServiceEnabled()) && StringUtils.isBlank(member.getClientId())) {
            errors.add(getError("missingClientId", langKey));
        } else if (!StringUtils.isBlank(member.getClientId())) {
            if (member.getClientId().startsWith(NEW_CLIENT_ID_PREFIX)) {
                Matcher newMatcher = NEW_CLIENT_ID_PATTERN.matcher(member.getClientId());
                if (!newMatcher.matches()) {
                    errors.add(getError("invalidClientId", langKey));
                }
            } else {
                Matcher oldMatcher = OLD_CLIENT_ID_PATTERN.matcher(member.getClientId());
                if (!oldMatcher.matches()) {
                    errors.add(getError("invalidClientId", langKey));
                }
            }
        }
    }

    private void validateSalesforceId(Member member, String langKey, List<String> errors) {
        if (StringUtils.isBlank(member.getSalesforceId())) {
            errors.add(getError("missingSalesforceId", langKey));
        } else {
            Optional<Member> matchingSalesforceId = memberRepository.findBySalesforceId(member.getSalesforceId());
            if (matchingSalesforceId.isPresent() && !matchingSalesforceId.get().getId().equals(member.getId())) {
                errors.add(getError("salesforceIdAlreadyExists", member.getSalesforceId(), langKey));
            }
        }
    }

    private void validateConsortiumLeadAndParentSalesforceId(Member member, String langKey, List<String> errors) {
        if (member.getIsConsortiumLead() == null) {
            errors.add(getError("missingConsortiumLead", langKey));
        } else if (member.getIsConsortiumLead() && !StringUtils.isBlank(member.getParentSalesforceId())
                && !member.getParentSalesforceId().equals(member.getSalesforceId())) {
            errors.add(getError("parentSalesforceIdNotAllowed", langKey));
        }
    }

    private void validateAssertionServiceEnabled(Member member, String langKey, List<String> errors) {
        if (member.getAssertionServiceEnabled() == null) {
            errors.add(getError("missingAssertionsEnabled", langKey));
        } else if (member.getAssertionServiceEnabled() && Boolean.TRUE.equals(member.getIsConsortiumLead()) && StringUtils.isBlank(member.getClientId())) {
            errors.add(getError("invalidAssertionsEnabled", langKey));
        }
    }

    private void validateType(Member member, String langKey, List<String> errors) {
        if (member.getType() != null) {
            if (!MEMBER_TYPE_BASIC.equals(member.getType()) && !MEMBER_TYPE_PREMIUM.equals(member.getType())) {
                errors.add(getError("invalidMemberType", langKey));
            }
        }
    }

    private String getError(String code, String langKey) {
        return getError(code, null, langKey);
    }

    private String getError(String code, String arg, String langKey) {
        return messageSource.getMessage("member.validation.error." + code, arg != null ? new Object[] { arg } : null, Locale.forLanguageTag(langKey));
    }

}
