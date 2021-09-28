package org.orcid.memberportal.service.assertion.repository;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.Assertion;

public interface AssertionRepositoryCustom {

    List<Assertion> findAllToUpdateInOrcidRegistry();

    List<Assertion> findAllToCreateInOrcidRegistry();

}
