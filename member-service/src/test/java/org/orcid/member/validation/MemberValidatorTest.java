package org.orcid.member.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.member.domain.Member;
import org.orcid.member.repository.MemberRepository;
import org.orcid.member.service.user.MemberServiceUser;
import org.springframework.context.MessageSource;

public class MemberValidatorTest {

	@Mock
	private MessageSource messageSource;
	
	@Mock
	private MemberRepository memberRepository;
	
	@InjectMocks
	private MemberValidator memberValidator;

	@Captor
	private ArgumentCaptor<String> errorMessagePropertyCaptor;
	
	@BeforeEach
	private void setUp() {
		MockitoAnnotations.initMocks(this);
		Mockito.when(messageSource.getMessage(Mockito.anyString(), Mockito.any(), Mockito.any(Locale.class)))
				.thenReturn("error-message");
		Mockito.when(memberRepository.findByClientName(Mockito.anyString())).thenReturn(Optional.empty());
		Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.empty());
	}

	@Test
	public void testValidateWithMissingAssertionsEnabled() {
		Member member = getMemberWithMissingAssertionsEnabled();
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(1, errors.size());
		Mockito.verify(messageSource, Mockito.times(1)).getMessage(errorMessagePropertyCaptor.capture(), Mockito.any(), Mockito.any());
		String propertyName = errorMessagePropertyCaptor.getValue();
		assertEquals("member.validation.error.missingAssertionsEnabled", propertyName);
	}
	
	@Test
	public void testValidateWithMissingSalesforceId() {
		Member member = getMemberWithMissingSalesforceId();
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(1, errors.size());
		Mockito.verify(messageSource, Mockito.times(1)).getMessage(errorMessagePropertyCaptor.capture(), Mockito.any(), Mockito.any());
		String propertyName = errorMessagePropertyCaptor.getValue();
		assertEquals("member.validation.error.missingSalesforceId", propertyName);
	}
	
	@Test
	public void testValidateWithMissingClientId() {
		Member member = getMemberWithMissingClientId();
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(1, errors.size());
		Mockito.verify(messageSource, Mockito.times(1)).getMessage(errorMessagePropertyCaptor.capture(), Mockito.any(), Mockito.any());
		String propertyName = errorMessagePropertyCaptor.getValue();
		assertEquals("member.validation.error.missingClientId", propertyName);
	}
	
	@Test
	public void testValidateWithMissingClientName() {
		Member member = getMemberWithMissingClientName();
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(1, errors.size());
		Mockito.verify(messageSource, Mockito.times(1)).getMessage(errorMessagePropertyCaptor.capture(), Mockito.any(), Mockito.any());
		String propertyName = errorMessagePropertyCaptor.getValue();
		assertEquals("member.validation.error.missingClientName", propertyName);
	}
	
	@Test
	public void testValidateWithInvalidClientId() {
		Member member = getMemberWithInvalidClientId();
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(1, errors.size());
		Mockito.verify(messageSource, Mockito.times(1)).getMessage(errorMessagePropertyCaptor.capture(), Mockito.any(), Mockito.any());
		String propertyName = errorMessagePropertyCaptor.getValue();
		assertEquals("member.validation.error.invalidClientId", propertyName);
	}
	
	@Test
	public void testValidateWithValidOldClientId() {
		Member member = getMemberWithValidOldClientId();
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(0, errors.size());
		Mockito.verify(messageSource, Mockito.never()).getMessage(Mockito.anyString(), Mockito.any(), Mockito.any());
	}
	
	@Test
	public void testValidateWithValidNewClientId() {
		Member member = getMemberWithValidNewClientId();
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(0, errors.size());
		Mockito.verify(messageSource, Mockito.never()).getMessage(Mockito.anyString(), Mockito.any(), Mockito.any());
	}
	
	@Test
	public void testValidateWithMissingConsortiumLead() {
		Member member = getMemberWithMissingConsortiumLead();
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(1, errors.size());
		Mockito.verify(messageSource, Mockito.times(1)).getMessage(errorMessagePropertyCaptor.capture(), Mockito.any(), Mockito.any());
		String propertyName = errorMessagePropertyCaptor.getValue();
		assertEquals("member.validation.error.missingConsortiumLead", propertyName);
	}
	
	@Test
	public void testValidateNonConsortiumLeadWithMissingParentSalesforceId() {
		Member member = getNonConsortiumLeadWithMissingParentSalesforceId();
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(1, errors.size());
		Mockito.verify(messageSource, Mockito.times(1)).getMessage(errorMessagePropertyCaptor.capture(), Mockito.any(), Mockito.any());
		String propertyName = errorMessagePropertyCaptor.getValue();
		assertEquals("member.validation.error.missingParentSalesforceId", propertyName);
	}
	
	@Test
	public void testValidateSalesforceIdExists() {
		Member member = getMemberWithValidNewClientId();
		Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.of(member));
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(1, errors.size());
		Mockito.verify(messageSource, Mockito.times(1)).getMessage(errorMessagePropertyCaptor.capture(), Mockito.any(), Mockito.any());
		String propertyName = errorMessagePropertyCaptor.getValue();
		assertEquals("member.validation.error.salesforceIdAlreadyExists", propertyName);
	}
	
	@Test
	public void testValidateNameExists() {
		Member member = getMemberWithValidNewClientId();
		Mockito.when(memberRepository.findByClientName(Mockito.anyString())).thenReturn(Optional.of(member));
		List<String> errors = memberValidator.validate(member, getUser(), true);
		assertEquals(1, errors.size());
		Mockito.verify(messageSource, Mockito.times(1)).getMessage(errorMessagePropertyCaptor.capture(), Mockito.any(), Mockito.any());
		String propertyName = errorMessagePropertyCaptor.getValue();
		assertEquals("member.validation.error.nameAlreadyExists", propertyName);
	}
	
	
	private Member getMemberWithValidOldClientId() {
		Member member = getMember();
		member.setClientId("1234-5678-1234-5678");
		return member;
	}
	
	private Member getMemberWithValidNewClientId() {
		Member member = getMember();
		member.setClientId("APP-1234567890123456");
		return member;
	}
	
	private Member getMemberWithMissingSalesforceId() {
		Member member = getMember();
		member.setSalesforceId(null);
		return member;
	}
	
	private Member getMemberWithMissingClientId() {
		Member member = getMember();
		member.setClientId(null);
		return member;
	}
	
	private Member getMemberWithMissingClientName() {
		Member member = getMember();
		member.setClientName(null);
		return member;
	}
	
	private Member getMemberWithInvalidClientId() {
		Member member = getMember();
		member.setClientId("invalid");
		return member;
	}

	private Member getNonConsortiumLeadWithMissingParentSalesforceId() {
		Member member = getMember();
		member.setIsConsortiumLead(false);
		member.setParentSalesforceId(null);
		return member;
	}

	private Member getMemberWithMissingConsortiumLead() {
		Member member = getMember();
		member.setIsConsortiumLead(null);
		return member;
	}

	private MemberServiceUser getUser() {
		MemberServiceUser user = new MemberServiceUser();
		user.setLangKey("en");
		return user;
	}

	private Member getMemberWithMissingAssertionsEnabled() {
		Member member = getMember();
		member.setAssertionServiceEnabled(null);
		return member;
	}

	private Member getMember() {
		Member member = new Member();
		member.setAssertionServiceEnabled(true);
		member.setClientId("APP-XXXXXXXXXXXXXXXX");
		member.setClientName("client");
		member.setCreatedDate(Instant.now());
		member.setLastModifiedDate(Instant.now());
		member.setCreatedBy("someone");
		member.setIsConsortiumLead(false);
		member.setParentSalesforceId("parent");
		member.setSalesforceId("salesforceId");
		member.setSuperadminEnabled(false);
		return member;
	}

}
