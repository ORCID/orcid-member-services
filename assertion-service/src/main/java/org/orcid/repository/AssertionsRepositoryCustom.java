package org.orcid.repository;

import java.util.List;

import org.orcid.domain.Assertion;
import org.springframework.data.mongodb.repository.Query;

public interface AssertionsRepositoryCustom {

    @Query("{putCode: {$ne:null}}")
    List<Assertion> findAllToUpdate();

}
