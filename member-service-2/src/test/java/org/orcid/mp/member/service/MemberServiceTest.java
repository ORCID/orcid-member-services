package org.orcid.mp.member.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.error.UnauthorizedMemberAccessException;
import org.orcid.mp.member.repository.MemberRepository;
import org.orcid.mp.member.salesforce.*;
import org.orcid.mp.member.security.MockSecurityContext;
import org.orcid.mp.member.upload.MemberCsvReader;
import org.orcid.mp.member.upload.MemberUpload;
import org.orcid.mp.member.validation.MemberValidation;
import org.orcid.mp.member.validation.MemberValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;

class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberCsvReader membersUploadReader;

    @Mock
    private UserService userService;

    @Mock
    private MemberValidator memberValidator;

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MailService mailService;

    @Captor
    private ArgumentCaptor<Member> memberCaptor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        SecurityContextHolder.setContext(new MockSecurityContext("me"));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findById(eq("memberId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(memberRepository.findById(eq("parentMemberId"))).thenReturn(Optional.of(getConsortiumLeadMember()));
        Mockito.when(memberRepository.findBySalesforceId(eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(memberRepository.findBySalesforceId(eq("parentSalesforceId"))).thenReturn(Optional.of(getConsortiumLeadMember()));
    }

    @Test
    void testCreateMember() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getValidValidation());
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
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getInvalidValidation("member-exists"));
        Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.of(getMember()));
        Member member = getMember();

        Assertions.assertThrows(BadRequestAlertException.class, () -> {
            memberService.createMember(member);
        });
    }

    @Test
    void testUpdateMember() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getValidValidation());
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
    void testUpdateMemberWithDuplicateName() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(getMember()));

        // return a record when name is checked against db
        Mockito.when(memberRepository.findByClientName(Mockito.anyString())).thenReturn(Optional.of(getMember()));

        Member member = getMember();
        member.setId("id");
        member.setSalesforceId("three");
        member.setClientName("new client name");

        Exception e = Assertions.assertThrows(BadRequestAlertException.class, () -> {
            memberService.updateMember(member);
        });

        assertThat(e.getMessage()).isEqualTo("Invalid member name");

        Mockito.verify(memberRepository, Mockito.never()).save(Mockito.any(Member.class));
    }

    @Test
    void testUpdateMemberWithMemberName() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(getMember()));
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });
        Member member = getMember();
        member.setClientName("a new member name");
        memberService.updateMember(member);

        // check assertion and user changes rolled back
        Mockito.verify(userService, Mockito.times(1)).updateUsersMemberNames(eq("memberId"), eq("a new member name"));
        Mockito.verify(memberRepository, Mockito.times(1)).save(memberCaptor.capture());

        Member saved = memberCaptor.getValue();
        assertThat(saved.getClientName()).isEqualTo("a new member name");
    }

    @Test
    void testUpdateMemberWithAssertionEnabledUpdate() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getValidValidation());

        Member existingMember = getMember();
        existingMember.setAssertionServiceEnabled(false);
        existingMember.setSalesforceId("salesforce-id");

        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(existingMember));
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });

        Member member = getMember();
        member.setId("id");
        member.setSalesforceId("salesforce-id");
        member.setAssertionServiceEnabled(true);
        memberService.updateMember(member);

        Mockito.verify(memberRepository, Mockito.times(1)).save(Mockito.any(Member.class));
    }

    @Test
    void testUpdateMemberWithCLUpdate() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getValidValidation());

        Member existingMember = getMember();
        existingMember.setAssertionServiceEnabled(false);
        existingMember.setIsConsortiumLead(false);
        existingMember.setSalesforceId("salesforce-id");

        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(existingMember));
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });

        Member member = getMember();
        member.setId("id");
        member.setSalesforceId("salesforce-id");
        member.setAssertionServiceEnabled(false);
        member.setIsConsortiumLead(true);
        memberService.updateMember(member);

        Mockito.verify(memberRepository, Mockito.times(1)).save(Mockito.any(Member.class));
    }

    @Test
    void testUpdateNonExistentMember() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getValidValidation());
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
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getInvalidValidation("some-error"));
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
    void testUploadMemberCSV() throws IOException {
        Mockito.when(membersUploadReader.readMemberUpload(Mockito.any(), Mockito.any(User.class))).thenReturn(getMemberUpload());
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(User.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findBySalesforceId(eq("one"))).thenReturn(Optional.empty());
        Member existing = getMember();
        existing.setId("two");
        Mockito.when(memberRepository.findBySalesforceId(eq("two"))).thenReturn(Optional.of(existing));
        Mockito.when(memberRepository.findById(eq("two"))).thenReturn(Optional.of(getMemberUpload().getMembers().get(1)));
        Mockito.when(memberRepository.findBySalesforceId(eq("three"))).thenReturn(Optional.empty());
        memberService.uploadMemberCSV(null);
        Mockito.verify(memberRepository, Mockito.times(3)).save(Mockito.any(Member.class));
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

    @Test
    void testUpdateMemberDefaultLanguage() throws UnauthorizedMemberAccessException {
        Member member = getMember();
        memberService.updateMemberDefaultLanguage("memberId", "en");
        Mockito.verify(memberRepository).save(memberCaptor.capture());
        Member captured = memberCaptor.getValue();
        assertThat(captured.getAssertionServiceEnabled()).isEqualTo(member.getAssertionServiceEnabled());
        assertThat(captured.getClientId()).isEqualTo(member.getClientId());
        assertThat(captured.getClientName()).isEqualTo(member.getClientName());
        assertThat(captured.getCreatedBy()).isEqualTo(member.getCreatedBy());
        assertThat(captured.getCreatedDate()).isEqualTo(member.getCreatedDate());
        assertThat(captured.getIsConsortiumLead()).isEqualTo(member.getIsConsortiumLead());
        assertThat(captured.getLastModifiedBy()).isEqualTo(member.getLastModifiedBy());
        assertThat(captured.getLastModifiedDate()).isEqualTo(member.getLastModifiedDate());
        assertThat(captured.getParentSalesforceId()).isEqualTo(member.getParentSalesforceId());
        assertThat(captured.getSalesforceId()).isEqualTo(member.getSalesforceId());
        assertThat(captured.getSuperadminEnabled()).isEqualTo(member.getSuperadminEnabled());
        assertThat(captured.getType()).isEqualTo(member.getType());

        assertThat(captured.getDefaultLanguage()).isEqualTo("en");
    }

    private MemberDetails getMemberDetails() {
        MemberDetails memberDetails = new MemberDetails();
        memberDetails.setBillingCountry("Denmark");
        memberDetails.setConsortiaLeadId(null);
        memberDetails.setConsortiaMember(false);
        memberDetails.setId("id");
        memberDetails.setLogoUrl("some/url/for/a/logo");
        memberDetails.setMemberType("Research Institute");
        memberDetails.setName("test member details");
        memberDetails.setPublicDisplayDescriptionHtml("<p>public display description</p>");
        memberDetails.setPublicDisplayEmail("orcid@testmember.com");
        memberDetails.setPublicDisplayName("public display name");
        memberDetails.setMembershipStartDateString("2022-01-01");
        memberDetails.setMembershipEndDateString("2027-01-01");
        memberDetails.setWebsite("https://website.com");
        return memberDetails;
    }

    private ConsortiumLeadDetails getConsortiumLeadDetails() {
        ConsortiumLeadDetails consortiumLeadDetails = new ConsortiumLeadDetails();
        consortiumLeadDetails.setBillingCountry("Denmark");
        consortiumLeadDetails.setConsortiaLeadId(null);
        consortiumLeadDetails.setConsortiaMember(false);
        consortiumLeadDetails.setId("id");
        consortiumLeadDetails.setLogoUrl("some/url/for/a/logo");
        consortiumLeadDetails.setMemberType("Research Institute");
        consortiumLeadDetails.setName("test member details");
        consortiumLeadDetails.setPublicDisplayDescriptionHtml("<p>public display description</p>");
        consortiumLeadDetails.setPublicDisplayEmail("orcid@testmember.com");
        consortiumLeadDetails.setPublicDisplayName("public display name");
        consortiumLeadDetails.setMembershipStartDateString("2022-01-01");
        consortiumLeadDetails.setMembershipEndDateString("2027-01-01");
        consortiumLeadDetails.setWebsite("https://website.com");

        ConsortiumMember member1 = new ConsortiumMember();
        member1.setSalesforceId("member1");
        ConsortiumMember.Metadata metadata1 = member1.new Metadata();
        metadata1.setName("member 1");
        member1.setMetadata(metadata1);

        ConsortiumMember member2 = new ConsortiumMember();
        member2.setSalesforceId("member2");
        ConsortiumMember.Metadata metadata2 = member2.new Metadata();
        metadata2.setName("member 2");
        member2.setMetadata(metadata2);

        consortiumLeadDetails.setConsortiumMembers(Arrays.asList(member1, member2));
        return consortiumLeadDetails;
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

    private User getUser() {
        User user = new User();
        user.setEmail("logged-in-user@orcid.org");
        user.setLangKey("en");
        user.setMemberId("memberId");
        user.setMemberName("member");
        return user;
    }

    private User getCLUser() {
        User user = new User();
        user.setEmail("logged-in-user@orcid.org");
        user.setLangKey("en");
        user.setMemberId("parentMemberId");
        user.setMemberName("member");
        return user;
    }

    private Member getMember() {
        Member member = new Member();
        member.setAssertionServiceEnabled(true);
        member.setClientId("XXXX-XXXX-XXXX-XXXX");
        member.setClientName("clientname");
        member.setIsConsortiumLead(false);
        member.setSalesforceId("salesforceId");
        member.setParentSalesforceId("parentSalesforceId");
        member.setId("memberId");
        return member;
    }

    private Member getConsortiumMember(String id) {
        Member member = new Member();
        member.setId(id);
        member.setSalesforceId(id);
        return member;
    }

    private Member getConsortiumLeadMember() {
        Member member = new Member();
        member.setAssertionServiceEnabled(true);
        member.setClientId("XXXX-XXXX-XXXX-XXXX");
        member.setClientName("clientname");
        member.setIsConsortiumLead(true);
        member.setSalesforceId("parentSalesforceId");
        member.setId("parentMemberId");;
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
