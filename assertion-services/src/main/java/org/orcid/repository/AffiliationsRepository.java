package org.orcid.repository;

import org.orcid.domain.Affiliation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AffiliationsRepository extends MongoRepository<Affiliation, String> {
    Page<Affiliation> findByOwnerId(String ownerId, Pageable pageable);
}
