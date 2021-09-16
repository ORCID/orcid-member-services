package org.orcid.repository;

import java.util.List;

import org.orcid.domain.Assertion;

public interface AssertionRepositoryCustom {

    List<Assertion> findAllToUpdateInOrcidRegistry();

    List<Assertion> findAllToCreateInOrcidRegistry();

}
