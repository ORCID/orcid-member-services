package org.orcid.member.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.orcid.member.domain.Member;
import org.orcid.member.repository.MemberRepository;
import org.orcid.member.service.user.MemberServiceUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.util.StringUtils;

@Component
public class MemberValidator {

	private static final String OLD_CLIENT_ID_PATTERN = "[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$";
	private static final String NEW_CLIENT_ID_PREFIX = "APP-";
	private static final String NEW_CLIENT_ID_PATTERN = NEW_CLIENT_ID_PREFIX + "[A-Z0-9]{16}$";
	private static final Pattern oldPattern = Pattern.compile(OLD_CLIENT_ID_PATTERN);
	private static final Pattern newPattern = Pattern.compile(NEW_CLIENT_ID_PATTERN);
	
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private MemberRepository memberRepository;
	
	public MemberValidation validate(Member member, MemberServiceUser user, boolean creatingMember) {
		List<String> errors = new ArrayList<>();
		validateAssertionServiceEnabled(member, user, errors);
		validateSalesforceId(member, user, errors, creatingMember);
		validateConsortiumLeadAndParentSalesforceId(member, user, errors);
		validateClientId(member, user, errors);
		validateClientName(member, user, errors, creatingMember);

		MemberValidation validation = new MemberValidation();
		validation.setValid(errors.isEmpty());
		validation.setErrors(errors);
		return validation;
	}
	
	private void validateClientName(Member member, MemberServiceUser user, List<String> errors,
			boolean creatingMember) {
		if (StringUtils.isBlank(member.getClientName())) {
			errors.add(getError("missingClientName", user));
		} else if (creatingMember && memberRepository.findByClientName(member.getClientName()).isPresent()) {
			errors.add(getError("nameAlreadyExists", member.getClientName(), user));
		}
	}

	private void validateClientId(Member member, MemberServiceUser user, List<String> errors) {
		if (StringUtils.isBlank(member.getClientId())) {
			errors.add(getError("missingClientId", user));
		} else {
			if (member.getClientId().startsWith(NEW_CLIENT_ID_PREFIX)) {
				Matcher newMatcher = newPattern.matcher(member.getClientId());
				if (!newMatcher.matches()) {
					errors.add(getError("invalidClientId", user));
				}
			} else {
				Matcher oldMatcher = oldPattern.matcher(member.getClientId());
				if (!oldMatcher.matches()) {
					errors.add(getError("invalidClientId", user));
				}
			}
		}
	}

	private void validateSalesforceId(Member member, MemberServiceUser user, List<String> errors, boolean creatingMember) {
		if (StringUtils.isBlank(member.getSalesforceId())) {
			errors.add(getError("missingSalesforceId", user));
		} else if (creatingMember && memberRepository.findBySalesforceId(member.getSalesforceId()).isPresent()) {
			errors.add(getError("salesforceIdAlreadyExists", member.getSalesforceId(), user));
		}
	}

	private void validateConsortiumLeadAndParentSalesforceId(Member member, MemberServiceUser user,
			List<String> errors) {
		if (member.getIsConsortiumLead() == null) {
			errors.add(getError("missingConsortiumLead", user));
		} else if (!member.getIsConsortiumLead() && StringUtils.isBlank(member.getParentSalesforceId())) {
			errors.add(getError("missingParentSalesforceId", user));
		}
	}

	private void validateAssertionServiceEnabled(Member member, MemberServiceUser user, List<String> errors) {
		if (member.getAssertionServiceEnabled() == null) {
			errors.add(getError("missingAssertionsEnabled", user));
		}
	}

	private String getError(String code, MemberServiceUser user) {
		return getError(code, null, user);
	}

	private String getError(String code, String arg, MemberServiceUser user) {
		return messageSource.getMessage("member.validation.error." + code, arg != null ? new Object[] { arg } : null,
				Locale.forLanguageTag(user.getLangKey()));
	}

}
