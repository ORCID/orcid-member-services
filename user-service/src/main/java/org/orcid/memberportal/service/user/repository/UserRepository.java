package org.orcid.memberportal.service.user.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.orcid.memberportal.service.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findOneByActivationKey(String activationKey);

    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);

    Optional<User> findOneByResetKey(String resetKey);

    Optional<User> findOneByEmailIgnoreCase(String email);

    Optional<User> findOneById(String id);

    Page<User> findAllByEmailNot(Pageable pageable, String email);

    List<User> findAllByEmailIgnoreCase(String email);

    List<User> findBySalesforceIdAndDeletedIsFalse(String salesforceId);
    
    Page<User> findBySalesforceIdAndDeletedIsFalse(Pageable pageable, String salesforceId);

    Page<User> findByDeletedIsFalseAndSalesforceIdAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndSalesforceIdAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndSalesforceIdAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndSalesforceIdAndEmailContainingIgnoreCase(
            Pageable pageable, String salesforceId1, String memberName, String salesforceId2, String firstName,
            String salesforceId3, String lastName, String salesforceId4, String email);

    Page<User> findByDeletedFalse(Pageable pageable);

    Optional<User> findOneByMainContactIsTrueAndSalesforceId(String salesforceId);

    List<User> findAllByMainContactIsTrueAndDeletedIsFalseAndSalesforceId(String salesforceId);

    List<User> findAllByMainContactIsTrueAndDeletedIsFalse();
    
    List<User> findAllByActivatedIsFalseAndDeletedIsFalse();

    Optional<User> findOneBySalesforceIdAndMainContactIsTrue(String salesforceId);

    List<User> findAllByAuthoritiesAndDeletedIsFalse(String role);

    Page<User> findByMemberName(Pageable pageable, String memberName);

    Page<User> findByDeletedIsFalseAndMemberNameContainingIgnoreCaseOrDeletedIsFalseAndFirstNameContainingIgnoreCaseOrDeletedIsFalseAndLastNameContainingIgnoreCaseOrDeletedIsFalseAndEmailContainingIgnoreCase(
            String memberName, String firstName, String lastName, String email, Pageable pageable);
}
