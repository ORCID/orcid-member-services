package org.orcid.user.repository;

import java.util.Optional;

import org.orcid.user.domain.MemberSettings;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for the MemberSettings entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MemberSettingsRepository extends MongoRepository<MemberSettings, String> {
    Optional<MemberSettings> findBySalesforceId(String salesforceId);
    Boolean existsBySalesforceId(String salesforceId);
}
