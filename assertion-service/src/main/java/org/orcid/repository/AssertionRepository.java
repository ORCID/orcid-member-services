package org.orcid.repository;

import java.util.List;
import java.util.Optional;

import org.orcid.domain.Assertion;
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
    
    @Query("{salesforceId: ?0}")    
    Page<Assertion> findBySalesforceId(String salesforceId, Pageable pageable);
    
    @Query("{salesforceId: ?0}")    
    List<Assertion> findBySalesforceId(String salesforceId, Sort sort);

    List<Assertion> findBySalesforceId(String salesforceId);

    List<Assertion> findByEmail(String email);
    
    List<Assertion> findByEmailAndSalesforceId(String email, String salesforceId);

    Optional<Assertion> findOneByEmailIgnoreCase(String email);
    

    List<Assertion> findByStatus(String status);

}
