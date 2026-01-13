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
import org.orcid.mp.member.error.UnauthorizedMemberAccessException;
import org.orcid.mp.member.pojo.AddConsortiumMember;
import org.orcid.mp.member.pojo.MemberContactUpdate;
import org.orcid.mp.member.pojo.RemoveConsortiumMember;
import org.orcid.mp.member.repository.MemberRepository;
import org.orcid.mp.member.rest.validation.MemberValidation;
import org.orcid.mp.member.rest.validation.MemberValidator;
import org.orcid.mp.member.salesforce.*;
import org.orcid.mp.member.security.AuthoritiesConstants;
import org.orcid.mp.member.security.EncryptUtil;
import org.orcid.mp.member.security.SecurityUtils;
import org.orcid.mp.member.upload.MemberCsvReader;
import org.orcid.mp.member.upload.MemberUpload;
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
    private MemberCsvReader memberUploadReader;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberValidator memberValidator;

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
            updateUserMemberNames(existingMember.getSalesforceId(), member.getClientName());
            existingMember.setClientName(member.getClientName());
        }
    }

    private void updateUserMemberNames(String salesforceId, String newClientName) {
        userService.updateUsersMemberNames(salesforceId, newClientName);
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

    public void updateUsersOnConsortiumLeadChange(Member member, Member existentMember) {
        if (existentMember == null) {
            throw new BadRequestAlertException("Invalid id");
        }
        if (Boolean.compare(existentMember.getIsConsortiumLead(), member.getIsConsortiumLead()) != 0) {
            List<User> usersBelongingToMember = userService.getUsersBySalesforceId(member.getSalesforceId());
            for (User user : usersBelongingToMember) {
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

    public void updateMemberDefaultLanguage(String salesforceId, String language) throws UnauthorizedMemberAccessException {
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
        User user = userService.getLoggedInUser();
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
        User user = userService.getLoggedInUser();

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
        User user = userService.getLoggedInUser();

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

    public List<Country> getSalesforceCountries() {
        return salesforceClient.getSalesforceCountries();
    }

    /**
     * validates whether current user can access member with specified salesforceId
     *
     * @param salesforceId
     */
    private void validateUserAccess(String salesforceId) throws UnauthorizedMemberAccessException {
        User user = userService.getLoggedInUser();
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
