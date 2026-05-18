package org.orcid.mp.assertion.repository;

import java.util.List;
import java.util.Optional;

import org.orcid.mp.assertion.domain.Assertion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AssertionRepository extends MongoRepository<Assertion, String>, AssertionRepositoryCustom {

    @Query("{ownerId: ?0}")
    Page<Assertion> findByOwnerId(String ownerId, Pageable pageable);

    @Query("{ownerId: ?0}")
    List<Assertion> findAllByOwnerId(String ownerId, Sort sort);

    Page<Assertion> findByMemberId(String memberId, Pageable pageable);

    List<Assertion> findByStatus(String status, Pageable pageable);

    Page<Assertion> findByMemberIdAndAffiliationSectionContainingIgnoreCaseOrMemberIdAndDepartmentNameContainingIgnoreCaseOrMemberIdAndOrgNameContainingIgnoreCaseOrMemberIdAndDisambiguatedOrgIdContainingIgnoreCaseOrMemberIdAndEmailContainingIgnoreCaseOrMemberIdAndOrcidIdContainingIgnoreCaseOrMemberIdAndRoleTitleContainingIgnoreCase(
            Pageable pageable, String memberId1, String affiliationSection, String memberId2, String departmentName, String memberId3, String orcName,
            String memberId4, String disambiguatedOrgId, String memberId5, String email, String memberId6, String orcidId, String memberId7,
            String roleTitle);

    @Query("{memberId: ?0}")
    List<Assertion> findByMemberId(String memberId, Sort sort);

    List<Assertion> findByMemberId(String memberId);

    List<Assertion> findByEmail(String email);

    List<Assertion> findByEmailAndMemberId(String email, String memberId);

    Optional<Assertion> findOneByEmailIgnoreCase(String email);

    List<Assertion> findByStatus(String status);

    List<Assertion> findAllByEmail(String email);

    Long countByEmailAndMemberId(String email, String memberId);

    List<Assertion> findByEmailAndMemberIdAndStatus(String email, String memberId, String status);

    @Query("{ addedToORCID: { $exists: false }, $or: [ { notificationSent: { $exists: true } }, { invitationSent: { $exists: true } } ] }")
    Page<Assertion> findNotificationResendCandidates(Pageable pageable);

    List<Assertion> findByStatusAndOrcidIdIsNull(String status);
}
