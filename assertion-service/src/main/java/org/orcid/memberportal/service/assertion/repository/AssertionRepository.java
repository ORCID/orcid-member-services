package org.orcid.memberportal.service.assertion.repository;

import java.util.List;
import java.util.Optional;

import org.orcid.memberportal.service.assertion.domain.Assertion;
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

    Page<Assertion> findBySalesforceId(String salesforceId, Pageable pageable);

    Page<Assertion> findBySalesforceIdAndAffiliationSectionContainingIgnoreCaseOrSalesforceIdAndDepartmentNameContainingIgnoreCaseOrSalesforceIdAndOrgNameContainingIgnoreCaseOrSalesforceIdAndDisambiguatedOrgIdContainingIgnoreCaseOrSalesforceIdAndEmailContainingIgnoreCaseOrSalesforceIdAndOrcidIdContainingIgnoreCaseOrSalesforceIdAndRoleTitleContainingIgnoreCase(
            Pageable pageable, String salesforceId1, String affiliationSection, String salesforceId2, String departmentName, String salesforceId3, String orcName,
            String salesforceId4, String disambiguatedOrgId, String salesforceId5, String email, String salesforceId6, String orcidId, String salesforceId7,
            String roleTitle);

    @Query("{salesforceId: ?0}")
    List<Assertion> findBySalesforceId(String salesforceId, Sort sort);

    List<Assertion> findBySalesforceId(String salesforceId);

    List<Assertion> findByEmail(String email);

    List<Assertion> findByEmailAndSalesforceId(String email, String salesforceId);

    Optional<Assertion> findOneByEmailIgnoreCase(String email);

    List<Assertion> findByStatus(String status);

    List<Assertion> findAllByEmail(String email);

}
