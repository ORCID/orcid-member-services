package org.orcid.mp.assertion.normalizer.org;

public interface OrgNormalizer {

    String normalizeOrgId(String orgId);

    String getOrgSource();

}