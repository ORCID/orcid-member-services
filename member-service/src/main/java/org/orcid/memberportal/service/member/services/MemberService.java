package org.orcid.memberportal.service.member.services;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.orcid.memberportal.service.member.client.SalesforceClient;
import org.orcid.memberportal.service.member.client.model.*;
import org.orcid.memberportal.service.member.domain.Member;
import org.orcid.memberportal.service.member.repository.MemberRepository;
import org.orcid.memberportal.service.member.security.AuthoritiesConstants;
import org.orcid.memberportal.service.member.security.EncryptUtil;
import org.orcid.memberportal.service.member.security.SecurityUtils;
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

    @Autowired
    private MailService mailService;

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
        existingMember.setParentSalesforceId(member.getParentSalesforceId());
        existingMember.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
        existingMember.setLastModifiedDate(Instant.now());
        existingMember.setAssertionServiceEnabled(member.getAssertionServiceEnabled());
        existingMember.setIsConsortiumLead(member.getIsConsortiumLead());

        // Check if name changed
        if (!existingMember.getClientName().equals(member.getClientName())) {
            Optional<Member> optionalMember = memberRepository.findByClientName(member.getClientName());
            if (optionalMember.isPresent()) {
                throw new BadRequestAlertException("Invalid member name", "member", "memberNameUsed.string");
            }
        }

        // Check if salesforceId changed
        if (!existingMember.getSalesforceId().equals(member.getSalesforceId())) {
            Optional<Member> optionalSalesforceId = memberRepository.findBySalesforceId(member.getSalesforceId());
            if (optionalSalesforceId.isPresent()) {
                throw new BadRequestAlertException("Invalid salesForceId", "member", "salesForceIdUsed.string");
            }

            String oldSalesforceId = existingMember.getSalesforceId();
            String newSalesforceId = member.getSalesforceId();

            try {
                // update affiliations and users associated with the member
                assertionService.updateAssertionsSalesforceId(oldSalesforceId, newSalesforceId);
            } catch (Exception e) {
                LOG.error("Error updating assertion salesforce ids", e);
                throw new RuntimeException(e);
            }

            try {
                userService.updateUsersSalesforceId(oldSalesforceId, newSalesforceId);
            } catch (Exception e) {
                LOG.error("Error updating users's salesforce id", e);
                LOG.error("Error updating users' salesforce id from {} to {}", oldSalesforceId, newSalesforceId);
                LOG.info("Attempting to perform salesforce id rollback on affiliations");
                assertionService.updateAssertionsSalesforceId(newSalesforceId, oldSalesforceId);
                LOG.info("Affiliation salesforce id rollback successfull");
                throw new RuntimeException(e);
            }
            existingMember.setSalesforceId(member.getSalesforceId());

            try {
                return memberRepository.save(existingMember);
            } catch (Exception e) {
                LOG.error("Error updating member", e);
                LOG.error("Error updating member's salesforce id from {} to {}", oldSalesforceId, newSalesforceId);
                LOG.info("Attempting to perform salesforce id rollback on affiliations");
                assertionService.updateAssertionsSalesforceId(newSalesforceId, oldSalesforceId);
                LOG.info("Affiliation salesforce id rollback successfull");

                LOG.info("Attempting to perform salesforce id rollback on users");
                userService.updateUsersSalesforceId(newSalesforceId, oldSalesforceId);
                LOG.info("User salesforce id rollback successfull");
                throw new RuntimeException(e);
            }
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

    public MemberDetails getMemberDetails(String salesforceId) throws UnauthorizedMemberAccessException {
        validateUserAccess(salesforceId);
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

    public Boolean updateMemberData(MemberUpdateData memberUpdateData, String salesforceId) throws UnauthorizedMemberAccessException {
        validateUserAccess(salesforceId);
        memberUpdateData.setSalesforceId(salesforceId);
        try {
            return salesforceClient.updatePublicMemberDetails(memberUpdateData);
        } catch (IOException e) {
            LOG.error("Error updating member contacts", e);
            throw new RuntimeException(e);
        }
    }

    public MemberContacts getCurrentMemberContacts(String salesforceId) throws UnauthorizedMemberAccessException {
        validateUserAccess(salesforceId);
        try {
            return salesforceClient.getMemberContacts(salesforceId);
        } catch (IOException e) {
            LOG.error("Error fetching member contacts from salesforce client", e);
            throw new RuntimeException(e);
        }
    }

    public MemberOrgIds getCurrentMemberOrgIds(String salesforceId) throws UnauthorizedMemberAccessException {
        validateUserAccess(salesforceId);
        try {
            return salesforceClient.getMemberOrgIds(salesforceId);
        } catch (IOException e) {
            LOG.error("Error fetching member org ids from salesforce client", e);
            throw new RuntimeException(e);
        }
    }

    public void updateMemberDefaultLanguage(String salesforceId, String language) {
        Optional<Member> optional = memberRepository.findBySalesforceId(salesforceId);
        if (optional.isPresent()) {
            Member member = optional.get();
            member.setDefaultLanguage(language);
            memberRepository.save(member);
        } else {
            throw new RuntimeException("Member not found");
        }
    }

    public void processMemberContact(MemberContactUpdate memberContactUpdate, String salesforceId) throws UnauthorizedMemberAccessException {
        validateUserAccess(salesforceId);
        MemberServiceUser user = userService.getLoggedInUser();
        memberContactUpdate.setRequestedByEmail(user.getEmail());
        memberContactUpdate.setRequestedByName(user.getFirstName() + " " + user.getLastName());
        memberContactUpdate.setRequestedByMember(user.getMemberName());

        if (memberContactUpdate.getContactEmail() == null) {
            mailService.sendAddContactEmail(memberContactUpdate);
        } else if (memberContactUpdate.getContactNewEmail() == null
            && memberContactUpdate.getContactNewName() == null
            && memberContactUpdate.getContactNewPhone() == null
            && memberContactUpdate.getContactNewRoles() == null
            && memberContactUpdate.getContactNewJobTitle() == null) {
            // no new data, must be remove operation
            mailService.sendRemoveContactEmail(memberContactUpdate);
        } else {
            mailService.sendUpdateContactEmail(memberContactUpdate);
        }
    }

    public void requestNewConsortiumMember(AddConsortiumMember addConsortiumMember) {
        MemberServiceUser user = userService.getLoggedInUser();

        Optional<Member> optionalMember = memberRepository.findBySalesforceId(user.getSalesforceId());
        Member member = optionalMember.get();
        if (!member.getIsConsortiumLead()) {
            throw new RuntimeException("Requesting member is not a consortium lead");
        }

        addConsortiumMember.setRequestedByEmail(user.getEmail());
        addConsortiumMember.setRequestedByName(user.getFirstName() + " " + user.getLastName());
        addConsortiumMember.setConsortium(user.getMemberName());

        mailService.sendAddConsortiumMemberEmail(addConsortiumMember);
    }

    public void requestRemoveConsortiumMember(RemoveConsortiumMember removeConsortiumMember) {
        MemberServiceUser user = userService.getLoggedInUser();

        Optional<Member> optionalMember = memberRepository.findBySalesforceId(user.getSalesforceId());
        Member member = optionalMember.get();
        if (!member.getIsConsortiumLead()) {
            throw new RuntimeException("Requesting member is not a consortium lead");
        }

        removeConsortiumMember.setRequestedByEmail(user.getEmail());
        removeConsortiumMember.setRequestedByName(user.getFirstName() + " " + user.getLastName());
        removeConsortiumMember.setConsortium(user.getMemberName());


        mailService.sendRemoveConsortiumMemberEmail(removeConsortiumMember);
    }

    /**
     * validates whether current user can access member with specified salesforceId
     *
     * @param salesforceId
     */
    private void validateUserAccess(String salesforceId) throws UnauthorizedMemberAccessException {
        MemberServiceUser user = userService.getLoggedInUser();
        if (!user.getSalesforceId().equals(salesforceId)) {
            // user not accessing own member

            Optional<Member> member = memberRepository.findBySalesforceId(salesforceId);
            if (!user.getSalesforceId().equals(member.get().getParentSalesforceId())) {
                // member not part of user's consortium
                LOG.warn("Illegal attempt by user {} to access member {}", user.getEmail(), salesforceId);
                throw new UnauthorizedMemberAccessException(user.getEmail(), salesforceId);
            }
        }
    }
}
