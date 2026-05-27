package org.orcid.mp.user.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.orcid.mp.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String>, CustomUserRepository {

    Optional<User> findOneByActivationKey(String activationKey);

    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);

    Optional<User> findOneByResetKey(String resetKey);

    Optional<User> findOneByEmailIgnoreCase(String email);

    Optional<User> findOneById(String id);

    Page<User> findAllByEmailNot(Pageable pageable, String email);

    List<User> findAllByEmailIgnoreCase(String email);

    List<User> findByMemberIdAndDeletedIsFalse(String memberId);

    Page<User> findByMemberIdAndDeletedIsFalse(Pageable pageable, String memberId);

    Page<User> findByDeletedIsFalseAndMemberIdAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndMemberIdAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndMemberIdAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndMemberIdAndEmailContainingIgnoreCase(
            Pageable pageable, String memberId1, String memberName, String memberId2, String firstName,
            String memberId3, String lastName, String memberId4, String email);

    Page<User> findByDeletedFalse(Pageable pageable);

    Optional<User> findOneByMainContactIsTrueAndMemberId(String memberId);

    List<User> findAllByMainContactIsTrueAndDeletedIsFalseAndMemberId(String memberId);

    List<User> findAllByMainContactIsTrueAndDeletedIsFalse();

    List<User> findAllByActivatedIsFalseAndDeletedIsFalse();

    Optional<User> findOneByMemberIdAndMainContactIsTrue(String memberId);

    Long countByAdminIsTrue();

    Page<User> findByMemberName(Pageable pageable, String memberName);

    Page<User> findByDeletedIsFalseAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndEmailContainingIgnoreCase(
            String memberName, String firstName, String lastName, String email, Pageable pageable);
}
