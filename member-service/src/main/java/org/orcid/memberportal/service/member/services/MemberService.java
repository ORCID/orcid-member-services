package org.orcid.memberportal.service.member.services;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.orcid.memberportal.service.member.client.SalesforceClient;
import org.orcid.memberportal.service.member.client.model.MemberContacts;
import org.orcid.memberportal.service.member.client.model.MemberDetails;
import org.orcid.memberportal.service.member.domain.Member;
import org.orcid.memberportal.service.member.repository.MemberRepository;
import org.orcid.memberportal.service.member.security.AuthoritiesConstants;
import org.orcid.memberportal.service.member.security.EncryptUtil;
import org.orcid.memberportal.service.member.security.SecurityUtils;
import org.orcid.memberportal.service.member.service.user.MemberServiceUser;
import org.orcid.memberportal.service.member.upload.MemberUpload;
import org.orcid.memberportal.service.member.upload.MembersUploadReader;
import org.orcid.memberportal.service.member.validation.MemberValidation;
import org.orcid.memberportal.service.member.validation.MemberValidator;
import org.orcid.memberportal.service.member.web.rest.errors.BadRequestAlertException;
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
    private MemberValidator memberValidator;

    @Autowired
    private AssertionService assertionService;

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private SalesforceClient salesforceClient;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

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

    public Boolean memberExists(String salesforceId) {
        Optional<Member> existingMember = memberRepository.findBySalesforceId(salesforceId);
        return existingMember.isPresent();
    }

    public Boolean memberSuperadminEnabled(String salesforceId) {
        Optional<Member> existingMember = memberRepository.findBySalesforceId(salesforceId);
        return existingMember.get().getSuperadminEnabled();
    }

    public Member createMember(Member member) {
        MemberValidation validation = memberValidator.validate(member, userService.getLoggedInUser());
        if (!validation.isValid()) {
            throw new BadRequestAlertException("Member invalid", "member", "validation.string");
        }

        Instant now = Instant.now();
        member.setCreatedDate(now);
        member.setLastModifiedDate(now);
        member.setCreatedBy(SecurityUtils.getCurrentUserLogin().get());
        member.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
        return memberRepository.save(member);
    }

    public Member updateMember(Member member) {
        MemberValidation validation = memberValidator.validate(member, userService.getLoggedInUser());
        if (!validation.isValid()) {
            throw new BadRequestAlertException("Member invalid", "member", "validation.string");
        }

        Optional<Member> optional = memberRepository.findById(member.getId());
        if (!optional.isPresent()) {
            throw new BadRequestAlertException("Invalid id", "member", "idunavailable.string");
        }

        Member existingMember = optional.get();
        existingMember.setClientId(member.getClientId());
        existingMember.setClientName(member.getClientName());
        existingMember.setIsConsortiumLead(member.getIsConsortiumLead());
        existingMember.setParentSalesforceId(member.getParentSalesforceId());
        existingMember.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
        existingMember.setLastModifiedDate(Instant.now());

        // Check if name changed
        if (!existingMember.getClientName().equals(member.getClientName())) {
            Optional<Member> optionalMember = memberRepository.findByClientName(member.getClientName());
            if (optionalMember.isPresent()) {
                throw new BadRequestAlertException("Invalid member name", "member", "memberNameUsed.string");
            }
        }

        // Check if salesforceId changed
        if (!existingMember.getSalesforceId().equals(member.getSalesforceId())) {
            Optional<Member> optionalSalesForceId = memberRepository.findBySalesforceId(member.getSalesforceId());
            if (optionalSalesForceId.isPresent()) {
                throw new BadRequestAlertException("Invalid salesForceId", "member", "salesForceIdUsed.string");
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

    public void deleteMember(String id) {
        Optional<Member> optional = memberRepository.findById(id);
        if (!optional.isPresent()) {
            throw new BadRequestAlertException("Invalid id", "member", "idunavailable.string");
        }
        List<MemberServiceUser> usersBelongingToMember = userService.getUsersBySalesforceId(optional.get().getSalesforceId());
        if (usersBelongingToMember != null && !usersBelongingToMember.isEmpty()) {
            assertionService.deleteAssertionsForSalesforceIn(optional.get().getSalesforceId());

            for (MemberServiceUser user : usersBelongingToMember) {
                LOG.warn("Deleting user: " + user.toString());
                userService.deleteUserById(user.getId(), true);
            }
        }
        memberRepository.deleteById(id);
    }

    public void updateUsersOnConsortiumLeadChange(Member member, Member existentMember) {
        if (existentMember == null) {
            throw new BadRequestAlertException("Invalid id", "member", "idunavailable.string");
        }
        if (Boolean.compare(existentMember.getIsConsortiumLead(), member.getIsConsortiumLead()) != 0) {
            List<MemberServiceUser> usersBelongingToMember = userService.getUsersBySalesforceId(member.getSalesforceId());
            for (MemberServiceUser user : usersBelongingToMember) {
                Set<String> authorities = user.getAuthorities();
                if (member.getIsConsortiumLead()) {
                    // add role consortium lead
                    authorities.add(AuthoritiesConstants.CONSORTIUM_LEAD);
                } else {
                    // remove role consortium lead
                    authorities.remove(AuthoritiesConstants.CONSORTIUM_LEAD);
                }
                user.setAuthorities(authorities);
                userService.updateUser(user);
            }
        }
    }

    public Optional<Member> getAuthorizedMemberForUser(String state) {
        String decryptState = encryptUtil.decrypt(state);
        String[] stateTokens = decryptState.split("&&");
        return getMember(stateTokens[0]);

    }

    public MemberDetails getCurrentMemberDetails() {
        String salesforceId = userService.getLoggedInUser().getSalesforceId();
        Member member = memberRepository.findBySalesforceId(salesforceId).orElseThrow();

        try {
            if (Boolean.TRUE.equals(member.getIsConsortiumLead())) {
                return salesforceClient.getConsortiumLeadDetails(salesforceId);
            } else {
                return salesforceClient.getMemberDetails(salesforceId);
            }
        } catch (IOException e) {
            LOG.error("Error fetching member details from salesforce client", e);
            throw new RuntimeException(e);
        }
    }

    public MemberContacts getCurrentMemberContacts() {
        String salesforceId = userService.getLoggedInUser().getSalesforceId();
        try {
            return salesforceClient.getMemberContacts(salesforceId);
        } catch (IOException e) {
            LOG.error("Error fetching member contacts from salesforce client", e);
            throw new RuntimeException(e);
        }
    }

}
