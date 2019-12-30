package org.orcid.repository;

import org.orcid.domain.Assertion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AffiliationsRepository extends MongoRepository<Assertion, String> {
    
    @Query("{ownerId: ?0}")    
    Page<Assertion> findByOwnerId(String ownerId, Pageable pageable);
}
