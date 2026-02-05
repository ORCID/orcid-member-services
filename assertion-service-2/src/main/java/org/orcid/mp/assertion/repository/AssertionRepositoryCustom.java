package org.orcid.mp.assertion.repository;

import java.util.Iterator;
import java.util.List;

import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.domain.MemberAssertionStatusCount;
import org.springframework.data.domain.Pageable;

public interface AssertionRepositoryCustom {

    List<Assertion> findAllToUpdateInOrcidRegistry(Pageable pageable);

    List<Assertion> findAllToCreateInOrcidRegistry(Pageable pageable);

    List<MemberAssertionStatusCount> getMemberAssertionStatusCounts();

    void updateStatusPendingToNotificationRequested(String salesforceId);

    Iterator<String> findDistinctEmailsWithNotificationRequested(String salesforceId);
}