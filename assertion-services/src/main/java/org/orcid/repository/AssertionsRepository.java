package org.orcid.repository;

import java.util.List;

import org.orcid.domain.Assertion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AssertionsRepository extends MongoRepository<Assertion, String> {
    
    @Query("{ownerId: ?0}")    
    Page<Assertion> findByOwnerId(String ownerId, Pageable pageable);
    
    @Query("{ownerId: ?0}")    
    List<Assertion> findAllByOwnerId(String ownerId);
}
