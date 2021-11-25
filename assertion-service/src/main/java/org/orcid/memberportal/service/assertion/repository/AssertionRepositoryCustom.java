package org.orcid.memberportal.service.assertion.repository;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.MemberAssertionStatusCount;

public interface AssertionRepositoryCustom {

    List<Assertion> findAllToUpdateInOrcidRegistry();

    List<Assertion> findAllToCreateInOrcidRegistry();

    List<MemberAssertionStatusCount> getMemberAssertionStatusCounts();

}
