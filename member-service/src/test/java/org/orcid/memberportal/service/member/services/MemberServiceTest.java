package org.orcid.memberportal.service.member.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.member.security.MockSecurityContext;
import org.orcid.memberportal.service.member.domain.Member;
import org.orcid.memberportal.service.member.repository.MemberRepository;
import org.orcid.memberportal.service.member.security.EncryptUtil;
import org.orcid.memberportal.service.member.service.user.MemberServiceUser;
import org.orcid.memberportal.service.member.upload.MemberUpload;
import org.orcid.memberportal.service.member.upload.MembersUploadReader;
import org.orcid.memberportal.service.member.validation.MemberValidation;
import org.orcid.memberportal.service.member.validation.MemberValidator;
import org.orcid.memberportal.service.member.web.rest.errors.BadRequestAlertException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MembersUploadReader membersUploadReader;

    @Mock
    private UserService userService;

    @Mock
    private AssertionService assertionService;

    @Mock
    private MemberValidator memberValidator;

    @InjectMocks
    private MemberService memberService;

    @Mock
    private EncryptUtil encryptUtil;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SecurityContextHolder.setContext(new MockSecurityContext("me"));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
    }

    @Test
    void testCreateMember() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });

        Member member = getMember();
        Member created = memberService.createMember(member);
        assertNotNull(created.getCreatedBy());
        assertNotNull(created.getCreatedDate());
        assertNotNull(created.getLastModifiedBy());
        assertNotNull(created.getLastModifiedDate());
        assertEquals(member.getClientName(), created.getClientName());
        assertEquals(member.getClientId(), created.getClientId());
        assertEquals(member.getSalesforceId(), created.getSalesforceId());
        assertEquals(member.getAssertionServiceEnabled(), created.getAssertionServiceEnabled());
        assertEquals(member.getIsConsortiumLead(), created.getIsConsortiumLead());
    }

    @Test
    void testCreateMemberWhenMemberExists() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getInvalidValidation("member-exists"));
        Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.of(getMember()));
        Member member = getMember();

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            memberService.createMember(member);
        });
    }

    @Test
    void testUpdateMember() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(getMember()));
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });

        Member member = getMember();
        member.setClientName("new name");
        member.setId("id");
        Member updated = memberService.updateMember(member);
        assertNotNull(updated.getLastModifiedBy());
        assertNotNull(updated.getLastModifiedDate());
        assertEquals(member.getClientName(), updated.getClientName());
        assertNotEquals(getMember().getClientName(), updated.getClientName());
        assertEquals(member.getClientId(), updated.getClientId());
        assertEquals(member.getSalesforceId(), updated.getSalesforceId());
        assertEquals(member.getAssertionServiceEnabled(), updated.getAssertionServiceEnabled());
        assertEquals(member.getIsConsortiumLead(), updated.getIsConsortiumLead());
    }

    @Test
    void testUpdateNonExistentMember() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });

        Member member = getMember();
        member.setClientName("new name");
        member.setId("id");

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            memberService.updateMember(member);
        });
    }

    @Test
    void testUpdateInvalidMember() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getInvalidValidation("some-error"));
        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(getMember()));

        Member member = getMember();
        member.setClientName("new name");
        member.setId("id");
        member.setSalesforceId(null);

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            memberService.updateMember(member);
        });
    }

    @Test
    void testMemberExists() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.of(getMember()));
        assertTrue(memberService.memberExists("anything"));

        Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.empty());
        assertFalse(memberService.memberExists("anything"));
    }

    @Test
    void testUploadMemberCSV() throws IOException {
        Mockito.when(membersUploadReader.readMemberUpload(Mockito.any(), Mockito.any(MemberServiceUser.class))).thenReturn(getMemberUpload());
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("one"))).thenReturn(Optional.empty());
        Member existing = getMember();
        existing.setId("two");
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("two"))).thenReturn(Optional.of(existing));
        Mockito.when(memberRepository.findById(Mockito.eq("two"))).thenReturn(Optional.of(getMemberUpload().getMembers().get(1)));
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("three"))).thenReturn(Optional.empty());
        memberService.uploadMemberCSV(null);
        Mockito.verify(memberRepository, Mockito.times(3)).save(Mockito.any(Member.class));
    }

    @Test
    void testGetAuthorizedMemberForUser() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        String email = "email@email.com";
        String encrypted = encryptUtil.encrypt("salesforceid" + "&&" + email);
        Mockito.when(assertionService.getOwnerIdForOrcidUser(Mockito.eq(encrypted))).thenReturn("ownerId");
        Mockito.when(userService.getSalesforceIdForUser(Mockito.eq("ownerId"))).thenReturn("salesforceId");
        Mockito.when(memberRepository.findById(Mockito.eq("salesforceid"))).thenReturn(Optional.empty());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceid"))).thenReturn(Optional.of(getMember()));
        Mockito.when(encryptUtil.decrypt(Mockito.eq(encrypted))).thenReturn("salesforceid" + "&&" + email);

        Optional<Member> optional = memberService.getAuthorizedMemberForUser(encrypted);

        assertTrue(optional.isPresent());

        Member member = optional.get();
        assertNotNull(member);
        assertEquals(getMember().getClientId(), member.getClientId());
        assertEquals(getMember().getClientName(), member.getClientName());
    }

    @Test
    void testGetAuthorizedMemberForUserBadEmail() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        String email = "email@email.com";
        String encrypted = encryptUtil.encrypt("salesforceid" + "&&" + email);
        Mockito.when(assertionService.getOwnerIdForOrcidUser(encrypted)).thenReturn(null);
        Mockito.when(encryptUtil.decrypt(Mockito.eq(encrypted))).thenReturn("salesforceid" + "&&" + email);
        Optional<Member> optional = memberService.getAuthorizedMemberForUser(encrypted);
        assertTrue(!optional.isPresent());
    }

    @Test
    void testGetMembers() {
        Mockito.when(memberRepository.findAll(Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(getMember(), getMember(), getMember())));
        Page<Member> page = memberService.getMembers(Mockito.mock(Pageable.class));
        assertNotNull(page);
        assertEquals(3, page.getTotalElements());
        Mockito.verify(memberRepository, Mockito.times(1)).findAll(Mockito.any(Pageable.class));

        Mockito.when(memberRepository.findByClientNameContainingIgnoreCaseOrSalesforceIdContainingIgnoreCaseOrParentSalesforceIdContainingIgnoreCase(Mockito.anyString(),
                Mockito.anyString(), Mockito.anyString(), Mockito.any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(getMember(), getMember(), getMember())));
        page = memberService.getMembers(Mockito.mock(Pageable.class), "test");
        assertNotNull(page);
        assertEquals(3, page.getTotalElements());
        Mockito.verify(memberRepository, Mockito.times(1)).findByClientNameContainingIgnoreCaseOrSalesforceIdContainingIgnoreCaseOrParentSalesforceIdContainingIgnoreCase(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(Pageable.class));
    }

    private MemberUpload getMemberUpload() {
        Member one = getMember();
        one.setSalesforceId("one");
        one.setClientName("one");
        one.setClientId("XXXX-XXXX-XXXX-XXX8");

        Member two = getMember();
        two.setSalesforceId("two");
        two.setClientName("two");
        two.setClientId("XXXX-XXXX-XXXX-XXX9");

        Member three = getMember();
        three.setSalesforceId("three");
        three.setClientName("three");
        three.setClientId("XXXX-XXXX-XXXX-XXX7");

        MemberUpload upload = new MemberUpload();
        upload.getMembers().add(one);
        upload.getMembers().add(two);
        upload.getMembers().add(three);

        return upload;
    }

    private MemberServiceUser getUser() {
        MemberServiceUser user = new MemberServiceUser();
        user.setEmail("logged-in-user@orcid.org");
        user.setLangKey("en");
        return user;
    }

    private Member getMember() {
        Member member = new Member();
        member.setAssertionServiceEnabled(true);
        member.setClientId("XXXX-XXXX-XXXX-XXXX");
        member.setClientName("clientname");
        member.setIsConsortiumLead(false);
        member.setSalesforceId("two");
        member.setParentSalesforceId("some parent");
        return member;
    }

    private MemberValidation getValidValidation() {
        MemberValidation validation = new MemberValidation();
        validation.setErrors(new ArrayList<>());
        validation.setValid(true);
        return validation;
    }

    private MemberValidation getInvalidValidation(String... errors) {
        MemberValidation validation = new MemberValidation();
        validation.setErrors(Arrays.asList(errors));
        validation.setValid(false);
        return validation;
    }
}
