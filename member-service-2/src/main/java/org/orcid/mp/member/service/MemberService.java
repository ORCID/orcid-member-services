package org.orcid.mp.member.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;


import org.orcid.mp.member.client.SalesforceClient;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.repository.MemberRepository;
import org.orcid.mp.member.validation.MemberValidation;
import org.orcid.mp.member.validation.MemberValidator;
import org.orcid.mp.member.salesforce.*;
import org.orcid.mp.member.security.AuthoritiesConstants;
import org.orcid.mp.member.security.SecurityUtils;
import org.orcid.mp.member.upload.MemberCsvReader;
import org.orcid.mp.member.upload.MemberUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private MemberCsvReader memberUploadReader;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberValidator memberValidator;

    @Autowired
    private SalesforceClient salesforceClient;

    @Autowired
    private MailService mailService;

    public MemberUpload uploadMemberCSV(InputStream inputStream) {
        LOG.info("Reading member CSV upload");
        MemberUpload upload = null;
        try {
            upload = memberUploadReader.readMemberUpload(inputStream, userService.getLoggedInUser());
        } catch (IOException e) {
            LOG.warn("Error reading member CSV upload");
            throw new RuntimeException(e);
        }

        if (upload.getErrors().length() > 0) {
            return upload;
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
            member.setId(optional.get().getId());
            return updateMember(member);
        }
    }

    public Member createMember(Member member) {
        MemberValidation validation = memberValidator.validate(member, userService.getLoggedInUser());
        if (!validation.isValid()) {
            throw new BadRequestAlertException("Member invalid");
        }

        Instant now = Instant.now();
        member.setCreatedDate(now);
        member.setLastModifiedDate(now);
        member.setCreatedBy(SecurityUtils.getCurrentUserLogin().get());
        member.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
        return memberRepository.save(member);
    }

    public Member updateMember(Member member) {
        Optional<Member> optional = memberRepository.findById(member.getId());
        validateMemberUpdate(member, optional);

        Member existingMember = optional.get();
        existingMember.setClientId(member.getClientId());
        existingMember.setParentSalesforceId(member.getParentSalesforceId());
        existingMember.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
        existingMember.setLastModifiedDate(Instant.now());
        existingMember.setAssertionServiceEnabled(member.getAssertionServiceEnabled());
        existingMember.setIsConsortiumLead(member.getIsConsortiumLead());

        propagateUpdatesAndSave(member, existingMember);

        return memberRepository.save(existingMember);
    }

    private void propagateUpdatesAndSave(Member member, Member existingMember) {
        if (!member.getClientName().equals(existingMember.getClientName())) {
            userService.updateUsersMemberNames(existingMember.getId(), member.getClientName());
            existingMember.setClientName(member.getClientName());
        }
    }

    private void validateMemberUpdate(Member member, Optional<Member> existingMember) {
        MemberValidation validation = memberValidator.validate(member, userService.getLoggedInUser());
        if (!validation.isValid()) {
            throw new BadRequestAlertException("Member invalid");
        }

        if (existingMember.isEmpty()) {
            throw new BadRequestAlertException("Invalid id");
        }

        // Check if name changed
        if (!existingMember.get().getClientName().equals(member.getClientName())) {
            Optional<Member> optionalMember = memberRepository.findByClientName(member.getClientName());
            if (optionalMember.isPresent()) {
                throw new BadRequestAlertException("Invalid member name");
            }
        }

        if (!existingMember.get().getSalesforceId().equals(member.getSalesforceId())) {
            Optional<Member> optionalSalesforceId = memberRepository.findBySalesforceId(member.getSalesforceId());
            if (optionalSalesforceId.isPresent()) {
                throw new BadRequestAlertException("Invalid salesForceId");
            }
        }
    }

    public MemberValidation validateMember(Member member) {
        return memberValidator.validate(member, userService.getLoggedInUser());
    }

    public Page<Member> getMembers(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    public Page<Member> getMembers(Pageable pageable, String filter) {
        return memberRepository.findByClientNameContainingIgnoreCaseOrSalesforceIdContainingIgnoreCaseOrParentSalesforceIdContainingIgnoreCase(filter, filter, filter,
            pageable);
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAllByOrderByClientNameAsc();
    }

    public Optional<Member> getMember(String id) {
        Optional<Member> member = memberRepository.findById(id);
        if (!member.isPresent()) {
            LOG.debug("Member settings not found for id {}, searching against salesforceId", id);
            member = memberRepository.findBySalesforceId(id);
        }
        return member;
    }

    public void updateMemberDefaultLanguage(String memberId, String language) {
        Optional<Member> optional = memberRepository.findById(memberId);
        if (optional.isPresent()) {
            Member member = optional.get();
            member.setDefaultLanguage(language);
            memberRepository.save(member);
        } else {
            throw new RuntimeException("Member " + memberId + " not found");
        }
    }
}
