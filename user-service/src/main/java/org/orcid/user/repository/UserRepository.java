package org.orcid.user.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.orcid.user.domain.User;
import org.orcid.user.service.cache.UserCaches;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(cacheNames = UserCaches.USERS_BY_EMAIL_CACHE)
    Optional<User> findOneByEmailIgnoreCase(String email);

    @Cacheable(cacheNames = UserCaches.USERS_BY_LOGIN_CACHE)
    Optional<User> findOneByLogin(String login);
    
    Optional<User> findOneById(String id);
    
    List<User> findAllByLoginOrEmail(String login, String email);

    Page<User> findAllByLoginNot(Pageable pageable, String login);
    
    List<User> findBySalesforceIdAndDeletedIsFalse(String salesforceId);
    
    Page<User> findBySalesforceIdAndDeletedIsFalse(Pageable pageable, String salesforceId);
    
    Page<User> findByDeletedFalse(Pageable pageable);
    
    Optional<User> findOneByMainContactIsTrueAndSalesforceId(String salesforceId);
    
    List<User> findAllByMainContactIsTrueAndDeletedIsFalseAndSalesforceId(String salesforceId);
    
    List<User> findAllByMainContactIsTrueAndDeletedIsFalse();
    
    Optional<User> findOneBySalesforceIdAndMainContactIsTrue(String salesforceId);
    
    List<User> findAllByAuthoritiesAndDeletedIsFalse(String role);
    
    Page<User> findByMemberName(Pageable pageable, String memberName);
}
