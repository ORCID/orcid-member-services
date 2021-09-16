package org.orcid.memberportal.service.assertion.repository;

import java.util.List;
import java.util.Optional;

import org.orcid.memberportal.service.assertion.domain.OrcidRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrcidRecordRepository extends MongoRepository<OrcidRecord, String> {

    Optional<OrcidRecord> findOneByEmail(String email);

    @Query(value = "{tokens: {salesforce_id: ?0}}")
    List<OrcidRecord> findAllToInvite(String salesforceId);

    @Query("{tokens: {salesforce_id: ?0}}")
    Page<OrcidRecord> findBySalesforceId(String salesforceId, Pageable pageable);
}
