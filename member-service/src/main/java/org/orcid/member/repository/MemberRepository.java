package org.orcid.member.repository;

import java.util.List;
import java.util.Optional;

import org.orcid.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the Members entity.
 */
@Repository
public interface MemberRepository extends MongoRepository<Member, String> {

    Optional<Member> findBySalesforceId(String salesforceId);

    Optional<Member> findByClientName(String clientName);

    Boolean existsBySalesforceId(String salesforceId);

    List<Member> findAllByOrderByClientNameAsc();

    Page<Member> findByClientNameContainingIgnoreCaseOrSalesforceIdContainingIgnoreCaseOrParentSalesforceIdContainingIgnoreCase(String clientName, String salesforceId,
            String parentSalesforceId, Pageable pageable);

}
