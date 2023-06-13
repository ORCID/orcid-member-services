package org.orcid.memberportal.service.member.services;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.memberportal.service.member.client.SalesforceClient;
import org.orcid.memberportal.service.member.client.model.ConsortiumLeadDetails;
import org.orcid.memberportal.service.member.client.model.ConsortiumMember;
import org.orcid.memberportal.service.member.client.model.MemberContact;
import org.orcid.memberportal.service.member.client.model.MemberContacts;
import org.orcid.memberportal.service.member.client.model.MemberDetails;
import org.orcid.memberportal.service.member.client.model.MemberOrgId;
import org.orcid.memberportal.service.member.client.model.MemberOrgIds;
import org.orcid.memberportal.service.member.client.model.MemberUpdateData;
import org.orcid.memberportal.service.member.domain.Member;
import org.orcid.memberportal.service.member.repository.MemberRepository;
import org.orcid.memberportal.service.member.security.EncryptUtil;
import org.orcid.memberportal.service.member.security.MockSecurityContext;
import org.orcid.memberportal.service.member.services.pojo.MemberServiceUser;
import org.orcid.memberportal.service.member.upload.MemberUpload;
import org.orcid.memberportal.service.member.upload.MembersUploadReader;
import org.orcid.memberportal.service.member.validation.MemberValidation;
import org.orcid.memberportal.service.member.validation.MemberValidator;
import org.orcid.memberportal.service.member.web.rest.errors.BadRequestAlertException;
import org.orcid.memberportal.service.member.web.rest.errors.UnauthorizedMemberAccessException;
import org.orcid.memberportal.service.member.web.rest.vm.AddConsortiumMember;
import org.orcid.memberportal.service.member.web.rest.vm.MemberContactUpdate;
import org.orcid.memberportal.service.member.web.rest.vm.RemoveConsortiumMember;
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

    @Mock
    private SalesforceClient salesforceClient;

    @InjectMocks
    private MemberService memberService;

    @Mock
    private EncryptUtil encryptUtil;

    @Mock
    private MailService mailService;

    @Captor
    private ArgumentCaptor<Member> memberCaptor;

    @Captor
    private ArgumentCaptor<MemberUpdateData> publicMemberDetailsCaptor;

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
    void testUpdateMemberWithSalesforceIdUpdateFailure_assertionFailure() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(getMember()));
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });
        Mockito.doThrow(new RuntimeException()).when(assertionService).updateAssertionsSalesforceId(Mockito.eq("two"), Mockito.eq("three"));

        Member member = getMember();
        member.setId("id");
        member.setSalesforceId("three");

        Assertions.assertThrows(RuntimeException.class, () -> {
            memberService.updateMember(member);
        });

        Mockito.verify(memberRepository, Mockito.never()).save(Mockito.any(Member.class));
        Mockito.verify(userService, Mockito.never()).updateUsersSalesforceId(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testUpdateMemberWithSalesforceIdUpdateFailure_userFailure() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(getMember()));
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });

        Mockito.doThrow(new RuntimeException()).when(userService).updateUsersSalesforceId(Mockito.eq("two"), Mockito.eq("three"));

        Member member = getMember();
        member.setId("id");
        member.setSalesforceId("three");

        Assertions.assertThrows(RuntimeException.class, () -> {
            memberService.updateMember(member);
        });

        Mockito.verify(memberRepository, Mockito.never()).save(Mockito.any(Member.class));

        // check assertion changes rolled back
        Mockito.verify(assertionService, Mockito.times(1)).updateAssertionsSalesforceId(Mockito.eq("two"), Mockito.eq("three"));
        Mockito.verify(assertionService, Mockito.times(1)).updateAssertionsSalesforceId(Mockito.eq("three"), Mockito.eq("two"));
        Mockito.verify(memberRepository, Mockito.never()).save(Mockito.any(Member.class));
    }

    @Test
    void testUpdateMemberWithSalesforceIdUpdateWithMemberFailure() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(getMember()));
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });

        Mockito.doThrow(new RuntimeException()).when(memberRepository).save(Mockito.any(Member.class));

        Member member = getMember();
        member.setId("id");
        member.setSalesforceId("three");

        Assertions.assertThrows(RuntimeException.class, () -> {
            memberService.updateMember(member);
        });

        // check assertion and user changes rolled back
        Mockito.verify(assertionService, Mockito.times(1)).updateAssertionsSalesforceId(Mockito.eq("two"), Mockito.eq("three"));
        Mockito.verify(assertionService, Mockito.times(1)).updateAssertionsSalesforceId(Mockito.eq("three"), Mockito.eq("two"));
        Mockito.verify(userService, Mockito.times(1)).updateUsersSalesforceId(Mockito.eq("two"), Mockito.eq("three"));
        Mockito.verify(userService, Mockito.times(1)).updateUsersSalesforceId(Mockito.eq("three"), Mockito.eq("two"));
    }

    @Test
    void testUpdateMemberWithSalesforceIdUpdate() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());
        Mockito.when(memberRepository.findById(Mockito.anyString())).thenReturn(Optional.of(getMember()));
        Mockito.when(memberRepository.save(Mockito.any(Member.class))).thenAnswer(new Answer<Member>() {
            @Override
            public Member answer(InvocationOnMock invocation) throws Throwable {
                return (Member) invocation.getArgument(0);
            }
        });
        Member member = getMember();
        member.setId("id");
        member.setSalesforceId("three");
        memberService.updateMember(member);

        // check assertion and user changes rolled back
        Mockito.verify(assertionService, Mockito.times(1)).updateAssertionsSalesforceId(Mockito.eq("two"), Mockito.eq("three"));
        Mockito.verify(userService, Mockito.times(1)).updateUsersSalesforceId(Mockito.eq("two"), Mockito.eq("three"));
        Mockito.verify(memberRepository, Mockito.times(1)).save(memberCaptor.capture());

        Member saved = memberCaptor.getValue();
        assertThat(saved.getSalesforceId()).isEqualTo("three");
    }

    @Test
    void testUpdateMemberWithAssertionEnabledUpdate() {
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());

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
        Mockito.when(memberValidator.validate(Mockito.any(Member.class), Mockito.any(MemberServiceUser.class))).thenReturn(getValidValidation());

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

    @Test
    void testGetCurrentMemberDetails() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(salesforceClient.getMemberDetails(Mockito.eq("salesforceId"))).thenReturn(getMemberDetails());

        MemberDetails memberDetails = memberService.getCurrentMemberDetails("salesforceId");
        assertThat(memberDetails).isNotNull();
        assertThat(memberDetails.getName()).isEqualTo("test member details");
        assertThat(memberDetails.getPublicDisplayName()).isEqualTo("public display name");
        assertThat(memberDetails.getWebsite()).isEqualTo("https://website.com");
        assertThat(memberDetails.getMembershipStartDateString()).isEqualTo("2022-01-01");
        assertThat(memberDetails.getMembershipEndDateString()).isEqualTo("2027-01-01");
        assertThat(memberDetails.getPublicDisplayEmail()).isEqualTo("orcid@testmember.com");
        assertThat(memberDetails.getConsortiaLeadId()).isNull();
        assertThat(memberDetails.isConsortiaMember()).isFalse();
        assertThat(memberDetails.getPublicDisplayDescriptionHtml()).isEqualTo("<p>public display description</p>");
        assertThat(memberDetails.getMemberType()).isEqualTo("Research Institute");
        assertThat(memberDetails.getLogoUrl()).isEqualTo("some/url/for/a/logo");
        assertThat(memberDetails.getBillingCountry()).isEqualTo("Denmark");
        assertThat(memberDetails.getId()).isEqualTo("id");
    }

    @Test
    void testGetCurrentMemberContacts() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(salesforceClient.getMemberContacts(Mockito.eq("salesforceId"))).thenReturn(getMemberContacts());

        MemberContacts memberContacts = memberService.getCurrentMemberContacts("salesforceId");
        assertThat(memberContacts).isNotNull();
        assertThat(memberContacts.getTotalSize()).isEqualTo(2);
        assertThat(memberContacts.getRecords()).isNotNull();
        assertThat(memberContacts.getRecords().size()).isEqualTo(2);
        assertThat(memberContacts.getRecords().get(0).getName()).isEqualTo("contact 1");
        assertThat(memberContacts.getRecords().get(0).getTitle()).isEqualTo("Dr");
        assertThat(memberContacts.getRecords().get(0).getPhone()).isEqualTo("123456789");
        assertThat(memberContacts.getRecords().get(0).getEmail()).isEqualTo("contact1@orcid.org");
        assertThat(memberContacts.getRecords().get(0).getRole()).isEqualTo("contact one role");
        assertThat(memberContacts.getRecords().get(0).getSalesforceId()).isEqualTo("salesforce-id");
        assertThat(memberContacts.getRecords().get(0).isVotingContact()).isEqualTo(false);
        assertThat(memberContacts.getRecords().get(1).getName()).isEqualTo("contact 2");
        assertThat(memberContacts.getRecords().get(1).getTitle()).isNull();
        assertThat(memberContacts.getRecords().get(1).getPhone()).isNull();
        assertThat(memberContacts.getRecords().get(1).getEmail()).isEqualTo("contact2@orcid.org");
        assertThat(memberContacts.getRecords().get(1).getRole()).isEqualTo("contact two role");
        assertThat(memberContacts.getRecords().get(1).getSalesforceId()).isEqualTo("salesforce-id");
        assertThat(memberContacts.getRecords().get(1).isVotingContact()).isEqualTo(true);
    }

    @Test
    void testGetCurrentMemberContacts_forConsortiumMemberByConsortiumLead() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getCLUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(salesforceClient.getMemberContacts(Mockito.eq("salesforceId"))).thenReturn(getMemberContacts());

        MemberContacts memberContacts = memberService.getCurrentMemberContacts("salesforceId");
        assertThat(memberContacts).isNotNull();
        assertThat(memberContacts.getTotalSize()).isEqualTo(2);
        assertThat(memberContacts.getRecords()).isNotNull();
        assertThat(memberContacts.getRecords().size()).isEqualTo(2);
        assertThat(memberContacts.getRecords().get(0).getName()).isEqualTo("contact 1");
        assertThat(memberContacts.getRecords().get(0).getTitle()).isEqualTo("Dr");
        assertThat(memberContacts.getRecords().get(0).getPhone()).isEqualTo("123456789");
        assertThat(memberContacts.getRecords().get(0).getEmail()).isEqualTo("contact1@orcid.org");
        assertThat(memberContacts.getRecords().get(0).getRole()).isEqualTo("contact one role");
        assertThat(memberContacts.getRecords().get(0).getSalesforceId()).isEqualTo("salesforce-id");
        assertThat(memberContacts.getRecords().get(0).isVotingContact()).isEqualTo(false);
        assertThat(memberContacts.getRecords().get(1).getName()).isEqualTo("contact 2");
        assertThat(memberContacts.getRecords().get(1).getTitle()).isNull();
        assertThat(memberContacts.getRecords().get(1).getPhone()).isNull();
        assertThat(memberContacts.getRecords().get(1).getEmail()).isEqualTo("contact2@orcid.org");
        assertThat(memberContacts.getRecords().get(1).getRole()).isEqualTo("contact two role");
        assertThat(memberContacts.getRecords().get(1).getSalesforceId()).isEqualTo("salesforce-id");
        assertThat(memberContacts.getRecords().get(1).isVotingContact()).isEqualTo(true);
    }

    @Test
    void testGetCurrentMemberContacts_unauthorizedMemberAccess() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("differentSalesforceId"))).thenReturn(Optional.of(getMember()));

        Assertions.assertThrows(UnauthorizedMemberAccessException.class, () -> {
            MemberContacts memberContacts = memberService.getCurrentMemberContacts("differentSalesforceId");
        });

        Mockito.verify(salesforceClient, Mockito.never()).getMemberContacts(Mockito.anyString());
    }

    @Test
    void testGetCurrentMemberOrgIds() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(salesforceClient.getMemberOrgIds(Mockito.eq("salesforceId"))).thenReturn(getMemberOrgIds());

        MemberOrgIds memberOrgIds = memberService.getCurrentMemberOrgIds("salesforceId");
        assertThat(memberOrgIds).isNotNull();
        assertThat(memberOrgIds.getTotalSize()).isEqualTo(2);
        assertThat(memberOrgIds.getRecords()).isNotNull();
        assertThat(memberOrgIds.getRecords().size()).isEqualTo(2);
        assertThat(memberOrgIds.getRecords().get(0).getType()).isEqualTo("Ringgold ID");
        assertThat(memberOrgIds.getRecords().get(0).getValue()).isEqualTo("9988776655");
        assertThat(memberOrgIds.getRecords().get(1).getType()).isEqualTo("GRID");
        assertThat(memberOrgIds.getRecords().get(1).getValue()).isEqualTo("grid.238252");
    }

    @Test
    void testGetCurrentMemberOrgIds_forConsortiumMemberByConsortiumLead() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getCLUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(salesforceClient.getMemberOrgIds(Mockito.eq("salesforceId"))).thenReturn(getMemberOrgIds());

        MemberOrgIds memberOrgIds = memberService.getCurrentMemberOrgIds("salesforceId");
        assertThat(memberOrgIds).isNotNull();
        assertThat(memberOrgIds.getTotalSize()).isEqualTo(2);
        assertThat(memberOrgIds.getRecords()).isNotNull();
        assertThat(memberOrgIds.getRecords().size()).isEqualTo(2);
        assertThat(memberOrgIds.getRecords().get(0).getType()).isEqualTo("Ringgold ID");
        assertThat(memberOrgIds.getRecords().get(0).getValue()).isEqualTo("9988776655");
        assertThat(memberOrgIds.getRecords().get(1).getType()).isEqualTo("GRID");
        assertThat(memberOrgIds.getRecords().get(1).getValue()).isEqualTo("grid.238252");
    }

    @Test
    void testGetCurrentMemberOrgIds_unauthorizedMemberAccess() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("differentSalesforceId"))).thenReturn(Optional.of(getMember()));

        Assertions.assertThrows(UnauthorizedMemberAccessException.class, () -> {
            MemberOrgIds memberOrgIds = memberService.getCurrentMemberOrgIds("differentSalesforceId");
        });

        Mockito.verify(salesforceClient, Mockito.never()).getMemberOrgIds(Mockito.anyString());

    }

    @Test
    void testGetCurrentMemberDetails_consortiumLead() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getConsortiumLeadMember()));
        Mockito.when(salesforceClient.getConsortiumLeadDetails(Mockito.eq("salesforceId"))).thenReturn(getConsortiumLeadDetails());

        ConsortiumLeadDetails consortiumLeadDetails = (ConsortiumLeadDetails) memberService.getCurrentMemberDetails("salesforceId");
        assertThat(consortiumLeadDetails).isNotNull();
        assertThat(consortiumLeadDetails.getName()).isEqualTo("test member details");
        assertThat(consortiumLeadDetails.getPublicDisplayName()).isEqualTo("public display name");
        assertThat(consortiumLeadDetails.getWebsite()).isEqualTo("https://website.com");
        assertThat(consortiumLeadDetails.getMembershipStartDateString()).isEqualTo("2022-01-01");
        assertThat(consortiumLeadDetails.getMembershipEndDateString()).isEqualTo("2027-01-01");
        assertThat(consortiumLeadDetails.getPublicDisplayEmail()).isEqualTo("orcid@testmember.com");
        assertThat(consortiumLeadDetails.getConsortiaLeadId()).isNull();
        assertThat(consortiumLeadDetails.isConsortiaMember()).isFalse();
        assertThat(consortiumLeadDetails.getPublicDisplayDescriptionHtml()).isEqualTo("<p>public display description</p>");
        assertThat(consortiumLeadDetails.getMemberType()).isEqualTo("Research Institute");
        assertThat(consortiumLeadDetails.getLogoUrl()).isEqualTo("some/url/for/a/logo");
        assertThat(consortiumLeadDetails.getBillingCountry()).isEqualTo("Denmark");
        assertThat(consortiumLeadDetails.getId()).isEqualTo("id");
        assertThat(consortiumLeadDetails.getConsortiumMembers().size()).isEqualTo(2);
        assertThat(consortiumLeadDetails.getConsortiumMembers().get(0).getSalesforceId()).isEqualTo("member1");
        assertThat(consortiumLeadDetails.getConsortiumMembers().get(0).getMetadata().getName()).isEqualTo("member 1");
        assertThat(consortiumLeadDetails.getConsortiumMembers().get(1).getSalesforceId()).isEqualTo("member2");
        assertThat(consortiumLeadDetails.getConsortiumMembers().get(1).getMetadata().getName()).isEqualTo("member 2");
    }

    @Test
    void testGetCurrentMemberDetails_forConsortiumMemberByConsortiumLead() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getCLUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(salesforceClient.getMemberDetails(Mockito.eq("salesforceId"))).thenReturn(getMemberDetails());

        MemberDetails memberDetails = memberService.getCurrentMemberDetails("salesforceId");
        assertThat(memberDetails).isNotNull();
        assertThat(memberDetails.getName()).isEqualTo("test member details");
        assertThat(memberDetails.getPublicDisplayName()).isEqualTo("public display name");
        assertThat(memberDetails.getWebsite()).isEqualTo("https://website.com");
        assertThat(memberDetails.getMembershipStartDateString()).isEqualTo("2022-01-01");
        assertThat(memberDetails.getMembershipEndDateString()).isEqualTo("2027-01-01");
        assertThat(memberDetails.getPublicDisplayEmail()).isEqualTo("orcid@testmember.com");
        assertThat(memberDetails.getConsortiaLeadId()).isNull();
        assertThat(memberDetails.isConsortiaMember()).isFalse();
        assertThat(memberDetails.getPublicDisplayDescriptionHtml()).isEqualTo("<p>public display description</p>");
        assertThat(memberDetails.getMemberType()).isEqualTo("Research Institute");
        assertThat(memberDetails.getLogoUrl()).isEqualTo("some/url/for/a/logo");
        assertThat(memberDetails.getBillingCountry()).isEqualTo("Denmark");
        assertThat(memberDetails.getId()).isEqualTo("id");
    }

    @Test
    void testGetCurrentMemberDetails_unauthorizedMemberAccess() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("differentSalesforceId"))).thenReturn(Optional.of(getMember()));

        Assertions.assertThrows(UnauthorizedMemberAccessException.class, () -> {
            MemberDetails memberDetails = memberService.getCurrentMemberDetails("differentSalesforceId");
        });

        Mockito.verify(salesforceClient, Mockito.never()).getMemberDetails(Mockito.anyString());
    }

    @Test
    void testUpdatePublicMemberDetails() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getConsortiumLeadMember()));
        Mockito.when(salesforceClient.updatePublicMemberDetails(Mockito.any(MemberUpdateData.class))).thenReturn(Boolean.TRUE);

        MemberUpdateData memberUpdateData = getPublicMemberDetails();
        memberService.updateMemberData(memberUpdateData, "salesforceId");

        Mockito.verify(salesforceClient).updatePublicMemberDetails(publicMemberDetailsCaptor.capture());
        MemberUpdateData details = publicMemberDetailsCaptor.getValue();
        assertThat(details).isNotNull();
        assertThat(details.getSalesforceId()).isEqualTo("salesforceId");
        assertThat(details.getName()).isEqualTo(memberUpdateData.getName());
        assertThat(details.getDescription()).isEqualTo(memberUpdateData.getDescription());
        assertThat(details.getWebsite()).isEqualTo(memberUpdateData.getWebsite());
        assertThat(details.getEmail()).isEqualTo(memberUpdateData.getEmail());
    }

    @Test
    void testUpdatePublicMemberDetails_forConsortiumMemberByConsortiumLead() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getCLUser());
        Mockito.when(salesforceClient.updatePublicMemberDetails(Mockito.any(MemberUpdateData.class))).thenReturn(Boolean.TRUE);
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));

        MemberUpdateData memberUpdateData = getPublicMemberDetails();
        memberService.updateMemberData(memberUpdateData, "salesforceId");

        Mockito.verify(salesforceClient).updatePublicMemberDetails(publicMemberDetailsCaptor.capture());
        MemberUpdateData details = publicMemberDetailsCaptor.getValue();
        assertThat(details).isNotNull();
        assertThat(details.getSalesforceId()).isEqualTo("salesforceId");
        assertThat(details.getName()).isEqualTo(memberUpdateData.getName());
        assertThat(details.getDescription()).isEqualTo(memberUpdateData.getDescription());
        assertThat(details.getWebsite()).isEqualTo(memberUpdateData.getWebsite());
        assertThat(details.getEmail()).isEqualTo(memberUpdateData.getEmail());
    }

    @Test
    void testUpdatePublicMemberDetails_unauthorizedMemberAccess() throws IOException, UnauthorizedMemberAccessException {
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(salesforceClient.updatePublicMemberDetails(Mockito.any(MemberUpdateData.class))).thenReturn(Boolean.TRUE);
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("differentSalesforceId"))).thenReturn(Optional.of(getMember()));

        MemberUpdateData memberUpdateData = getPublicMemberDetails();
        Assertions.assertThrows(UnauthorizedMemberAccessException.class, () -> {
            memberService.updateMemberData(memberUpdateData, "differentSalesforceId");
        });

        Mockito.verify(salesforceClient, Mockito.never()).updatePublicMemberDetails(Mockito.any());
    }

    @Test
    void testUpdateMemberDefaultLanguage() {
        Member member = getMember();
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(member));
        memberService.updateMemberDefaultLanguage("salesforceId", "en");
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

    @Test
    void testProcessMemberContact_add() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactNewEmail("a.contact@email.com");

        memberService.processMemberContact(update, "salesforceId");

        Mockito.verify(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.verify(userService, Mockito.times(2)).getLoggedInUser();
    }

    @Test
    void testProcessMemberContact_add_forConsortiumMemberByConsortiumLead() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getCLUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactNewEmail("a.contact@email.com");

        memberService.processMemberContact(update, "salesforceId");

        Mockito.verify(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.verify(userService, Mockito.times(2)).getLoggedInUser();
    }

    @Test
    void testProcessMemberContact_add_unauthorizedMemberAccess() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("differentSalesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactNewEmail("a.contact@email.com");

        Assertions.assertThrows(UnauthorizedMemberAccessException.class, () -> {
            memberService.processMemberContact(update, "differentSalesforceId");
        });

        Mockito.verify(mailService, Mockito.never()).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
    }

    @Test
    void testProcessMemberContact_remove() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactEmail("a.contact@email.com");

        memberService.processMemberContact(update, "salesforceId");

        Mockito.verify(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.verify(userService, Mockito.times(2)).getLoggedInUser();
    }

    @Test
    void testProcessMemberContact_remove_forConsortiumMemberByConsortiumLead() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getCLUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactEmail("a.contact@email.com");

        memberService.processMemberContact(update, "salesforceId");

        Mockito.verify(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.verify(userService, Mockito.times(2)).getLoggedInUser();
    }

    @Test
    void testProcessMemberContact_remove_unauthorizedMemberAccess() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("differentSalesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactEmail("a.contact@email.com");

        Assertions.assertThrows(UnauthorizedMemberAccessException.class, () -> {
            memberService.processMemberContact(update, "differentSalesforceId");
        });

        Mockito.verify(mailService, Mockito.never()).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
    }

    @Test
    void testProcessMemberContact_update() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactEmail("a.contact@email.com");
        update.setContactNewEmail("a.new.contact@email.com");

        memberService.processMemberContact(update, "salesforceId");

        Mockito.verify(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.verify(userService, Mockito.times(2)).getLoggedInUser();
    }

    @Test
    void testProcessMemberContact_update_forConsortiumMemberByConsortiumLead() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getCLUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactEmail("a.contact@email.com");
        update.setContactNewEmail("a.new.contact@email.com");

        memberService.processMemberContact(update, "salesforceId");

        Mockito.verify(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.verify(userService, Mockito.times(2)).getLoggedInUser();
    }

    @Test
    void testProcessMemberContact_update_unauthorizedMemberAccess() throws UnauthorizedMemberAccessException {
        Mockito.doNothing().when(mailService).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendAddContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.doNothing().when(mailService).sendRemoveContactEmail(Mockito.any(MemberContactUpdate.class));
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("differentSalesforceId"))).thenReturn(Optional.of(getMember()));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());

        MemberContactUpdate update = new MemberContactUpdate();
        update.setContactEmail("a.contact@email.com");
        update.setContactNewEmail("a.new.contact@email.com");

        Assertions.assertThrows(UnauthorizedMemberAccessException.class, () -> {
            memberService.processMemberContact(update, "differentSalesforceId");
        });

        Mockito.verify(mailService, Mockito.never()).sendUpdateContactEmail(Mockito.any(MemberContactUpdate.class));
    }

    @Test
    void testAddConsortiumMember() {
        Mockito.doNothing().when(mailService).sendAddConsortiumMemberEmail(Mockito.any(AddConsortiumMember.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getConsortiumLeadMember()));

        AddConsortiumMember addConsortiumMember = new AddConsortiumMember();
        addConsortiumMember.setOrgName("new org name");

        memberService.requestNewConsortiumMember(addConsortiumMember);

        Mockito.verify(mailService).sendAddConsortiumMemberEmail(Mockito.any(AddConsortiumMember.class));
        Mockito.verify(userService).getLoggedInUser();
        Mockito.verify(memberRepository).findBySalesforceId(Mockito.eq("salesforceId"));
    }

    @Test
    void testRemoveConsortiumMember_nonCL() {
        Mockito.doNothing().when(mailService).sendRemoveConsortiumMemberEmail(Mockito.any(RemoveConsortiumMember.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));

        RemoveConsortiumMember removeConsortiumMember = new RemoveConsortiumMember();
        removeConsortiumMember.setOrgName("old org name");

        Assertions.assertThrows(RuntimeException.class, () -> {
            memberService.requestRemoveConsortiumMember(removeConsortiumMember);
        });
    }

    @Test
    void testRemoveConsortiumMember() {
        Mockito.doNothing().when(mailService).sendRemoveConsortiumMemberEmail(Mockito.any(RemoveConsortiumMember.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getConsortiumLeadMember()));

        RemoveConsortiumMember removeConsortiumMember = new RemoveConsortiumMember();
        removeConsortiumMember.setOrgName("old org name");

        memberService.requestRemoveConsortiumMember(removeConsortiumMember);

        Mockito.verify(mailService).sendRemoveConsortiumMemberEmail(Mockito.any(RemoveConsortiumMember.class));
        Mockito.verify(userService).getLoggedInUser();
        Mockito.verify(memberRepository).findBySalesforceId(Mockito.eq("salesforceId"));
    }

    @Test
    void testAddConsortiumMember_nonCL() {
        Mockito.doNothing().when(mailService).sendAddConsortiumMemberEmail(Mockito.any(AddConsortiumMember.class));
        Mockito.when(userService.getLoggedInUser()).thenReturn(getUser());
        Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(Optional.of(getMember()));

        AddConsortiumMember addConsortiumMember = new AddConsortiumMember();
        addConsortiumMember.setOrgName("new org name");

        Assertions.assertThrows(RuntimeException.class, () -> {
            memberService.requestNewConsortiumMember(addConsortiumMember);
        });
    }

    private MemberUpdateData getPublicMemberDetails() {
        MemberUpdateData memberUpdateData = new MemberUpdateData();
        memberUpdateData.setName("test member details");
        memberUpdateData.setWebsite("https://website.com");
        memberUpdateData.setDescription("test");
        memberUpdateData.setEmail("email@orcid.org");
        return memberUpdateData;
    }

    private MemberOrgIds getMemberOrgIds() {
        MemberOrgId orgId1 = new MemberOrgId();
        orgId1.setType("Ringgold ID");
        orgId1.setValue("9988776655");

        MemberOrgId orgId2 = new MemberOrgId();
        orgId2.setType("GRID");
        orgId2.setValue("grid.238252");

        MemberOrgIds memberOrgIds = new MemberOrgIds();
        memberOrgIds.setTotalSize(2);
        memberOrgIds.setRecords(Arrays.asList(orgId1, orgId2));

        return memberOrgIds;
    }

    private MemberContacts getMemberContacts() {
        MemberContacts memberContacts = new MemberContacts();

        MemberContact contact1 = new MemberContact();
        contact1.setName("contact 1");
        contact1.setTitle("Dr");
        contact1.setPhone("123456789");
        contact1.setEmail("contact1@orcid.org");
        contact1.setRole("contact one role");
        contact1.setSalesforceId("salesforce-id");
        contact1.setVotingContact(false);

        MemberContact contact2 = new MemberContact();
        contact2.setName("contact 2");
        contact2.setEmail("contact2@orcid.org");
        contact2.setRole("contact two role");
        contact2.setSalesforceId("salesforce-id");
        contact2.setVotingContact(true);

        memberContacts.setTotalSize(2);
        memberContacts.setRecords(Arrays.asList(contact1, contact2));

        return memberContacts;
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

    private MemberServiceUser getUser() {
        MemberServiceUser user = new MemberServiceUser();
        user.setEmail("logged-in-user@orcid.org");
        user.setLangKey("en");
        user.setSalesforceId("salesforceId");
        user.setMemberName("member");
        return user;
    }

    private MemberServiceUser getCLUser() {
        MemberServiceUser user = new MemberServiceUser();
        user.setEmail("logged-in-user@orcid.org");
        user.setLangKey("en");
        user.setSalesforceId("parentSalesforceId");
        user.setMemberName("member");
        return user;
    }

    private Member getMember() {
        Member member = new Member();
        member.setAssertionServiceEnabled(true);
        member.setClientId("XXXX-XXXX-XXXX-XXXX");
        member.setClientName("clientname");
        member.setIsConsortiumLead(false);
        member.setSalesforceId("two");
        member.setParentSalesforceId("parentSalesforceId");
        return member;
    }

    private Member getConsortiumLeadMember() {
        Member member = new Member();
        member.setAssertionServiceEnabled(true);
        member.setClientId("XXXX-XXXX-XXXX-XXXX");
        member.setClientName("clientname");
        member.setIsConsortiumLead(true);
        member.setSalesforceId("two");
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
