package org.orcid.mp.user.repository;

import org.springframework.stereotype.Repository;

@Repository
public interface CustomUserRepository {

    boolean updateMemberNames(String salesforceId, String newMemberName);
}
