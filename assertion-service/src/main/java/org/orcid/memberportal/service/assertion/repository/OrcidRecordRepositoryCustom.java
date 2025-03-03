package org.orcid.memberportal.service.assertion.repository;

import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.MemberAssertionStatusCount;
import org.springframework.data.domain.Pageable;

import java.util.Iterator;
import java.util.List;

public interface OrcidRecordRepositoryCustom {

    void updateTokenSalesforceIds(String oldSalesforceId, String newSalesforceId);

}
