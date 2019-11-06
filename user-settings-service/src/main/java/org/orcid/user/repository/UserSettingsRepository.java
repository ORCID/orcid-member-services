package org.orcid.user.repository;

import org.orcid.user.domain.UserSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


/**
 * Spring Data MongoDB repository for the MemberServicesUser entity.
 */
@Repository
public interface UserSettingsRepository extends MongoRepository<UserSettings, String> {

}
