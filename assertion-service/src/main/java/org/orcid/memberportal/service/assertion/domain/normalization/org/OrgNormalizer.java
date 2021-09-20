package org.orcid.memberportal.service.assertion.domain.normalization.org;

public interface OrgNormalizer {
    
    String normalizeOrgId(String orgId);
    
    String getOrgSource();

}
