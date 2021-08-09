package org.orcid.repository.impl;

import java.util.List;

import org.orcid.domain.Assertion;

public interface AssertionRepositoryCustom {

    List<Assertion> findAllToUpdateInOrcidRegistry();
    
    List<Assertion> findAllToCreateInOrcidRegistry();

}
