package org.orcid.member.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.orcid.member.domain.Member;
import org.orcid.member.repository.MemberRepository;
import org.orcid.member.security.SecurityUtils;
import org.orcid.member.service.user.MemberServiceUser;
import org.orcid.member.upload.MemberUpload;
import org.orcid.member.upload.MembersUploadReader;
import org.orcid.member.web.rest.MemberValidator;
import org.orcid.member.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Service class for managing members.
 */
@Service
public class MemberService {

	private static final Logger LOG = LoggerFactory.getLogger(MemberService.class);

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private MembersUploadReader memberUploadReader;

	@Autowired
	private UserService userService;

	@Autowired
	private AssertionService assertionService;

	@Value("${jhipster.clientApp.name}")
	private String applicationName;

	public MemberUpload uploadMemberCSV(InputStream inputStream) {
		LOG.info("Reading member CSV upload");
		MemberUpload upload = null;
		try {
			upload = memberUploadReader.readMemberUpload(inputStream);
		} catch (IOException e) {
			LOG.warn("Error reading member CSV upload");
			throw new RuntimeException(e);
		}

		for (Member member : upload.getMembers()) {
			createOrUpdateMember(member);
		}
		return upload;
	}

	public Member createOrUpdateMember(Member member) {
		Optional<Member> optional = memberRepository.findBySalesforceId(member.getSalesforceId());

		if (!optional.isPresent()) {
			return createMember(member);
		} else {
			return updateMember(member);
		}
	}

	public Boolean memberExists(String salesforceId) {
		Optional<Member> existingMember = memberRepository.findBySalesforceId(salesforceId);
		return existingMember.isPresent();
	}

	public Member createMember(Member member) {
		if (member.getId() != null) {
			throw new BadRequestAlertException("A new member cannot already have an ID", "member", "idexists");
		}
		Optional<Member> optional = memberRepository.findBySalesforceId(member.getSalesforceId());
		if (optional.isPresent()) {
			throw new BadRequestAlertException("A member with that salesforce id already exists", "member", "idexists");
		}
		if (!MemberValidator.validate(member)) {
			throw new BadRequestAlertException("Member invalid", "member", "invalid");
		}

		if (member.getCreatedBy() == null) {
			member.setCreatedBy(SecurityUtils.getCurrentUserLogin().get());
			member.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
		}

		if (member.getCreatedDate() == null) {
			Instant now = Instant.now();
			member.setCreatedDate(now);
			member.setLastModifiedDate(now);
		}
		return memberRepository.save(member);
	}

	public Member updateMember(Member member) {
		if (member.getId() == null) {
			throw new BadRequestAlertException("Invalid id", "member", "idnull");
		}

		Optional<Member> optional = memberRepository.findById(member.getId());
		if (!optional.isPresent()) {
			throw new BadRequestAlertException("Invalid id", "member", "idunavailable");
		}

		if (!MemberValidator.validate(member)) {
			// what to do here? return member object with errors for ui? something
			// consistent
			throw new BadRequestAlertException("Invalid member", "member", null);
		}

		Instant now = Instant.now();
		member.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
		member.setLastModifiedDate(now);

		Member existingMember = optional.get();
		existingMember.setAssertionServiceEnabled(member.getAssertionServiceEnabled());
		existingMember.setClientId(member.getClientId());
		existingMember.setClientName(member.getClientName());
		existingMember.setIsConsortiumLead(member.getIsConsortiumLead());
		existingMember.setParentSalesforceId(member.getParentSalesforceId());
		existingMember.setLastModifiedBy(member.getLastModifiedBy());
		existingMember.setLastModifiedDate(member.getLastModifiedDate());

		// Check if salesforceId changed
		if (!existingMember.getSalesforceId().equals(member.getSalesforceId())) {
			// update users associated with member
			List<MemberServiceUser> usersBelongingToMember = userService
					.getUsersBySalesforceId(optional.get().getSalesforceId());
			for (MemberServiceUser user : usersBelongingToMember) {
				user.setSalesforceId(member.getSalesforceId());
				user.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
				user.setLastModifiedDate(now);
				userService.updateUser(user);
			}
		}

		return memberRepository.save(existingMember);
	}

	public Page<Member> getAllMembers(Pageable pageable) {
		return memberRepository.findAll(pageable);
	}

	public Optional<Member> getMember(String id) {
		Optional<Member> member = memberRepository.findById(id);
		if (!member.isPresent()) {
			LOG.debug("Member settings now found for id {}, searching against salesforceId", id);
			member = memberRepository.findBySalesforceId(id);
		}
		return member;
	}

	public void deleteMember(String id) {
		Optional<Member> optional = memberRepository.findById(id);
		if (!optional.isPresent()) {
			throw new BadRequestAlertException("Invalid id", "member", "idunavailable");
		}
		List<MemberServiceUser> usersBelongingToMember = userService
				.getUsersBySalesforceId(optional.get().getSalesforceId());
		if (usersBelongingToMember != null && !usersBelongingToMember.isEmpty()) {
		    assertionService.deleteAssertionsForSalesforceIn(optional.get().getSalesforceId());
		    
		    for (MemberServiceUser user : usersBelongingToMember) {
		        LOG.warn("Deleting user: " + user.toString());
		        userService.deleteUserById(user.getId());
		    }
		}
		memberRepository.deleteById(id);
	}

	public Optional<Member> getAuthorizedMemberForUser(String encryptedEmail) {
		// send encrypted email to assertion service for decryption and get owner of
		// orcid user
		String ownerId = assertionService.getOwnerIdForOrcidUser(encryptedEmail);
		if (ownerId != null) {
			String salesforceId = userService.getSalesforceIdForUser(ownerId);
			return getMember(salesforceId);
		} else {
			return Optional.empty();
		}
	}

}
