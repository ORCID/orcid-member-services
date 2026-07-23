package org.orcid.mp.member.service;

import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.repository.MemberRepository;
import org.orcid.mp.member.security.SecurityUtils;
import org.orcid.mp.member.upload.MemberCsvReader;
import org.orcid.mp.member.upload.MemberUpload;
import org.orcid.mp.member.validation.MemberValidation;
import org.orcid.mp.member.validation.MemberValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
            return createMember(member, SecurityUtils.getCurrentUserLogin().get());
        } else {
            member.setId(optional.get().getId());
            return updateMember(member, SecurityUtils.getCurrentUserLogin().get());
        }
    }

    public Member createMember(Member member, String createdBy) {
        MemberValidation validation = memberValidator.validate(member, member.getDefaultLanguage() != null ? member.getDefaultLanguage() : "en");
        if (!validation.isValid()) {
            LOG.warn("Member invalid {}", validation.getErrors().toString());
            throw new BadRequestAlertException("Member invalid");
        }

        Instant now = Instant.now();
        member.setCreatedDate(now);
        member.setLastModifiedDate(now);
        member.setCreatedBy(createdBy);
        member.setLastModifiedBy(createdBy);
        return memberRepository.save(member);
    }

    public Member updateMember(Member member, String updatedBy) {
        Optional<Member> optional = memberRepository.findById(member.getId());
        validateMemberUpdate(member, optional);

        Member dbCopy = optional.get();
        if (memberBasBeenUpdated(member, dbCopy)) {
            Member existingMember = optional.get();
            existingMember.setClientId(member.getClientId());
            existingMember.setParentSalesforceId(member.getParentSalesforceId());
            existingMember.setLastModifiedBy(updatedBy);
            existingMember.setLastModifiedDate(Instant.now());
            existingMember.setAssertionServiceEnabled(member.getAssertionServiceEnabled());
            existingMember.setIsConsortiumLead(member.getIsConsortiumLead());
            existingMember.setActive(member.isActive());
            existingMember.setActivatedDate(member.getActivatedDate());
            existingMember.setDeactivatedDate(member.getDeactivatedDate());
            propagateUpdatesAndSave(member, existingMember);
            return memberRepository.save(existingMember);
        }

        return member;
    }

    public MemberValidation validateMember(Member member) {
        return memberValidator.validate(member, userService.getLoggedInUser().getLangKey());
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
            member.setLastModifiedDate(Instant.now());
            member.setLastModifiedBy(SecurityUtils.getCurrentUserLogin().get());
            memberRepository.save(member);
        } else {
            throw new RuntimeException("Member " + memberId + " not found");
        }
    }

    public void addParent(String childSalesforceId, String parentSalesforceId, String lastModifiedBy) {
        LOG.info("Adding parent {} to member {}", parentSalesforceId, childSalesforceId);
        Optional<Member> childMember = memberRepository.findBySalesforceId(childSalesforceId);
        if (!childMember.isPresent()) {
            LOG.warn("Child member {} not found", childSalesforceId);
            return;
        }

        Member consortiumMember = childMember.get();
        if (parentSalesforceId.equals(consortiumMember.getParentSalesforceId())) {
            LOG.debug("Parent {} already set for consortium member {}", childSalesforceId);
            return;
        }

        consortiumMember.setParentSalesforceId(parentSalesforceId);
        consortiumMember.setLastModifiedDate(Instant.now());
        consortiumMember.setLastModifiedBy(lastModifiedBy);
        memberRepository.save(consortiumMember);
    }

    public void removeParent(String salesforceId, String lastModifiedBy) {
        LOG.info("Removing parent from member {}", salesforceId);
        Optional<Member> member = memberRepository.findBySalesforceId(salesforceId);
        if (member.isPresent()) {
            Member child = member.get();
            child.setParentSalesforceId(null);
            child.setLastModifiedDate(Instant.now());
            child.setLastModifiedBy(lastModifiedBy);
            memberRepository.save(child);
        } else {
            LOG.warn("Child member {} not found", salesforceId);
        }
    }

    public void removeParentFromMembersNoLongerPartOfConsortium(String salesforceId, Set<String> consortiumSalesforceIds, String lastModifiedBy) {
        List<Member> members = memberRepository.findAllByParentSalesforceId(salesforceId);
        members.forEach(m -> {
            if (!consortiumSalesforceIds.contains(m.getSalesforceId())) {
                LOG.info("Removing parent from member {}", m.getId());
                m.setParentSalesforceId(null);
                m.setLastModifiedDate(Instant.now());
                m.setLastModifiedBy(lastModifiedBy);
                memberRepository.save(m);
            }
        });
    }

    private void propagateUpdatesAndSave(Member member, Member existingMember) {
        if (!member.getClientName().equals(existingMember.getClientName())) {
            userService.updateUsersMemberNames(existingMember.getId(), member.getClientName());
            existingMember.setClientName(member.getClientName());
        }
    }

    private void validateMemberUpdate(Member member, Optional<Member> existingMember) {
        MemberValidation validation = memberValidator.validate(member, member.getDefaultLanguage() != null ? member.getDefaultLanguage() : "en");
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
                throw new BadRequestAlertException("Invalid salesforceId");
            }
        }
    }

    private boolean memberBasBeenUpdated(Member latestCopy, Member dbCopy) {
        return !Objects.equals(latestCopy.getClientId(), dbCopy.getClientId())
                || !Objects.equals(latestCopy.getSalesforceId(), dbCopy.getSalesforceId())
                || !Objects.equals(latestCopy.getParentSalesforceId(), dbCopy.getParentSalesforceId())
                || !Objects.equals(latestCopy.getAssertionServiceEnabled(), dbCopy.getAssertionServiceEnabled())
                || !Objects.equals(latestCopy.getIsConsortiumLead(), dbCopy.getIsConsortiumLead())
                || !Objects.equals(latestCopy.getSuperadminEnabled(), dbCopy.getSuperadminEnabled())
                || !Objects.equals(latestCopy.getClientName(), dbCopy.getClientName())
                || latestCopy.isActive() != dbCopy.isActive() // primitive boolean, safe to compare directly
                || !Objects.equals(latestCopy.getActivatedDate(), dbCopy.getActivatedDate())
                || !Objects.equals(latestCopy.getDeactivatedDate(), dbCopy.getDeactivatedDate())
                || !Objects.equals(latestCopy.getDefaultLanguage(), dbCopy.getDefaultLanguage())
                || !Objects.equals(latestCopy.getType(), dbCopy.getType());
    }

}
