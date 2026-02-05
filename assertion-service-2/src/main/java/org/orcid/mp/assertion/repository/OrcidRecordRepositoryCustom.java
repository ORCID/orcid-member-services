package org.orcid.mp.assertion.repository;

public interface OrcidRecordRepositoryCustom {

    void updateTokenSalesforceIds(String oldSalesforceId, String newSalesforceId);

}
