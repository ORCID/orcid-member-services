package org.orcid.member.service;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.member.repository.MemberRepository;
import org.orcid.member.upload.MembersUploadReader;

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
	}

	@Test
	void test() {
		fail("Not yet implemented");
	}

}
