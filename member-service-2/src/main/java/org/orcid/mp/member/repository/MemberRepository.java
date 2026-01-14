package org.orcid.mp.member.repository;

import java.util.List;
import java.util.Optional;

import org.orcid.mp.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {

    Optional<Member> findBySalesforceId(String salesforceId);

    Optional<Member> findByClientName(String clientName);

    Boolean existsBySalesforceId(String salesforceId);

    List<Member> findAllByOrderByClientNameAsc();

    Page<Member> findByClientNameContainingIgnoreCaseOrSalesforceIdContainingIgnoreCaseOrParentSalesforceIdContainingIgnoreCase(String clientName, String salesforceId,
                                                                                                                                String parentSalesforceId, Pageable pageable);
}
