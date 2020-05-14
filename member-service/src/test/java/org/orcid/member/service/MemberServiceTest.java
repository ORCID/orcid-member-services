package org.orcid.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
import org.orcid.member.domain.Member;
import org.orcid.member.repository.MemberRepository;
import org.orcid.member.security.MockSecurityContext;
import org.orcid.member.upload.MemberUpload;
import org.orcid.member.upload.MembersUploadReader;
import org.orcid.member.web.rest.errors.BadRequestAlertException;
import org.springframework.security.core.context.SecurityContextHolder;

class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private MembersUploadReader membersUploadReader;

	@InjectMocks
	private MemberService memberService;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		SecurityContextHolder.setContext(new MockSecurityContext("me"));
	}

	@Test
	void testCreateMember() {
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
		Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.of(getMember()));
		Member member = getMember();

		Assertions.assertThrows(BadRequestAlertException.class, () -> {
			memberService.createMember(member);
		});
	}

	@Test
	void testUpdateMember() {
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
		Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.of(getMember()));
		assertTrue(memberService.memberExists("anything"));
		
		Mockito.when(memberRepository.findBySalesforceId(Mockito.anyString())).thenReturn(Optional.empty());
		assertFalse(memberService.memberExists("anything"));
	}

	@Test
	void testUploadMemberCSV() throws IOException {
		Mockito.when(membersUploadReader.readMemberUpload(Mockito.any())).thenReturn(getMemberUpload());
		Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("one"))).thenReturn(Optional.empty());
		Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("two"))).thenReturn(Optional.of(getMember()));
		Mockito.when(memberRepository.findById(Mockito.eq("two"))).thenReturn(Optional.of(getMemberUpload().getMembers().get(1)));
		Mockito.when(memberRepository.findBySalesforceId(Mockito.eq("three"))).thenReturn(Optional.empty());
		memberService.uploadMemberCSV(null);
		Mockito.verify(memberRepository, Mockito.times(3)).save(Mockito.any(Member.class));
	}

	private MemberUpload getMemberUpload() {
		Member one = getMember();
		one.setSalesforceId("one");
		one.setClientName("one");
		
		Member two = getMember();
		two.setId("two");
		two.setSalesforceId("two");
		two.setClientName("two");
		
		Member three = getMember();
		three.setSalesforceId("three");
		three.setClientName("three");
		
		
		MemberUpload upload = new MemberUpload();
		upload.getMembers().add(one);
		upload.getMembers().add(two);
		upload.getMembers().add(three);
		
		return upload;
	}

	private Member getMember() {
		Member member = new Member();
		member.setAssertionServiceEnabled(true);
		member.setClientId("clientid");
		member.setClientName("clientname");
		member.setIsConsortiumLead(false);
		member.setSalesforceId("salesforceId");
		return member;
	}

}
