package org.orcid.repository;

import java.util.List;
import java.util.Optional;

import org.orcid.domain.OrcidRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrcidRecordRepository extends MongoRepository<OrcidRecord, String> {

    Optional<OrcidRecord> findOneByEmail(String email);
    
    @Query("{id_token: ?0}")
    Optional<OrcidRecord> findOneByIdToken(String idToken);
    
    @Query(value = "{owner_id: ?0, id_token: null}")
    List<OrcidRecord> findAllToInvite(String ownerId);
}
