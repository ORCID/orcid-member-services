package org.orcid.memberportal.service.assertion.repository;

import java.util.List;

import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.MemberAssertionStatusCount;
import org.springframework.data.domain.Pageable;

public interface AssertionRepositoryCustom {
    
    List<Assertion> findAllToUpdateInOrcidRegistry(Pageable pageable);

    List<Assertion> findAllToCreateInOrcidRegistry(Pageable pageable);

    List<MemberAssertionStatusCount> getMemberAssertionStatusCounts();

    void updateStatusPendingToNotificationRequested(String salesforceId);

}
