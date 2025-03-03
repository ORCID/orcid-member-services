package org.orcid.memberportal.service.user.repository;

import org.orcid.memberportal.service.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for the {@link User} entity.
 */
@Repository
public interface CustomUserRepository {

    boolean updateMemberNames(String salesforceId, String oldMemberName, String newMemberName);
}
