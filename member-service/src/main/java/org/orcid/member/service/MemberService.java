package org.orcid.member.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.orcid.member.domain.Member;
import org.orcid.member.repository.MemberRepository;
import org.orcid.member.security.EncryptUtil;
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
    
    @Autowired
    private EncryptUtil encryptUtil;
    

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
            member.setId(optional.get().getId());
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
            throw new BadRequestAlertException("A member with that salesforce id already exists", "member", "salesForceIdUsed");
        }
        Optional<Member> optionalMemberName = memberRepository.findByClientName(member.getClientName());
        if (optionalMemberName.isPresent()) {
            throw new BadRequestAlertException("A member with that name already exists", "member", "memberNameUsed");
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
            // what to do here? return member object with errors for ui?
            // something
            // consistent
            throw new BadRequestAlertException("Invalid member", "member", null);
        }

        Instant now = Instant.now();
        member.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
        member.setLastModifiedDate(now);

        Member existingMember = optional.get();
        existingMember.setClientId(member.getClientId());
        existingMember.setClientName(member.getClientName());
        existingMember.setIsConsortiumLead(member.getIsConsortiumLead());
        existingMember.setParentSalesforceId(member.getParentSalesforceId());
        existingMember.setLastModifiedBy(member.getLastModifiedBy());
        existingMember.setLastModifiedDate(member.getLastModifiedDate());

        // Check if name changed
        if (!existingMember.getClientName().equals(member.getClientName())) {
            Optional<Member> optionalMember = memberRepository.findByClientName(member.getClientName());
            if (optionalMember.isPresent()) {
                throw new BadRequestAlertException("Invalid member name", "member", "memberNameUsed");
            }
        }

        // Check if salesforceId changed
        if (!existingMember.getSalesforceId().equals(member.getSalesforceId())) {
            Optional<Member> optionalSalesForceId = memberRepository.findBySalesforceId(member.getSalesforceId());
            if (optionalSalesForceId.isPresent()) {
                throw new BadRequestAlertException("Invalid salesForceId", "member", "salesForceIdUsed");
            }
            // update affiliations associated with the member
            String oldSalesForceId = existingMember.getSalesforceId();
            existingMember.setSalesforceId(member.getSalesforceId());
            member = memberRepository.save(existingMember);
            assertionService.updateAssertionsSalesforceId(oldSalesForceId, member.getSalesforceId());
            userService.updateUserSalesforceIdOrAssertion(oldSalesForceId, member.getSalesforceId());
        }

        if (!existingMember.getAssertionServiceEnabled().equals(member.getAssertionServiceEnabled())) {
            existingMember.setAssertionServiceEnabled(member.getAssertionServiceEnabled());
            member = memberRepository.save(existingMember);
            userService.updateUserSalesforceIdOrAssertion(existingMember.getSalesforceId(), member.getSalesforceId());
            return member;
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
        List<MemberServiceUser> usersBelongingToMember = userService.getUsersBySalesforceId(optional.get().getSalesforceId());
        if (usersBelongingToMember != null && !usersBelongingToMember.isEmpty()) {
            assertionService.deleteAssertionsForSalesforceIn(optional.get().getSalesforceId());

            for (MemberServiceUser user : usersBelongingToMember) {
                LOG.warn("Deleting user: " + user.toString());
                userService.deleteUserById(user.getId());
            }
        }
        memberRepository.deleteById(id);
    }

    public Optional<Member> getAuthorizedMemberForUser(String state) { 
        String decryptState = encryptUtil.decrypt(state);
        String[] stateTokens = decryptState.split("&&");      
        return getMember(stateTokens[0]);
        
    }

}
