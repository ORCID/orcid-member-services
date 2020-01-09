package org.orcid.repository;

import java.util.Optional;

import org.orcid.domain.OrcidRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrcidRecordRepository extends MongoRepository<OrcidRecord, String> {

    Optional<OrcidRecord> findOneByEmail(String email);
}
