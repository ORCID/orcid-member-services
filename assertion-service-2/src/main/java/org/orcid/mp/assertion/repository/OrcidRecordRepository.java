package org.orcid.mp.assertion.repository;

import java.util.List;
import java.util.Optional;

import org.orcid.mp.assertion.domain.OrcidRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrcidRecordRepository extends MongoRepository<OrcidRecord, String> {

    Optional<OrcidRecord> findOneByEmail(String email);

    @Query(value = "{tokens: {member_id: ?0}}")
    List<OrcidRecord> findAllToInvite(String memberId);

    @Query("{tokens: {member_id: ?0}}")
    Page<OrcidRecord> findByMemberId(String memberId, Pageable pageable);

}
