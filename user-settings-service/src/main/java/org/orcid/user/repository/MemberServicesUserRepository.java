package org.orcid.user.repository;

import org.orcid.user.domain.MemberServicesUser;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


/**
 * Spring Data MongoDB repository for the MemberServicesUser entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MemberServicesUserRepository extends MongoRepository<MemberServicesUser, String> {

}
