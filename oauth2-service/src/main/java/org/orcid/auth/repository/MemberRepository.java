package org.orcid.auth.repository;

import java.util.Optional;

import org.orcid.auth.domain.Member;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the MemberSettings entity.
 */
@Repository
public interface MemberRepository extends MongoRepository<Member, String> {
    Optional<Member> findBySalesforceId(String salesforceId);
    Boolean existsBySalesforceId(String salesforceId);
}
