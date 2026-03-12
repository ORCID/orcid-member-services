package org.orcid.mp.assertion.util;

import com.google.common.base.Objects;
import org.apache.commons.lang3.StringUtils;
import org.orcid.mp.assertion.domain.AffiliationSection;
import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.normalizer.AssertionNormalizer;
import org.orcid.mp.assertion.repository.AssertionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

public class AssertionUtils {
    private static final String GRID_BASE_URL = "https://www.grid.ac/";
    private static final String GRID_BASE_URL_INSTITUTES = "https://www.grid.ac/institutes/";
    private static final String GRID_BASE_URL_ALT = "https://grid.ac/";
    private static final String GRID_BASE_URL_INSTITUTES_ALT = "https://grid.ac/institutes/";

    public static String stripGridURL(String gridIdentifier) {
        if (!StringUtils.isBlank(gridIdentifier)) {
            if (gridIdentifier.startsWith(GRID_BASE_URL_INSTITUTES)) {
                gridIdentifier = gridIdentifier.substring(GRID_BASE_URL_INSTITUTES.length());
            } else if (gridIdentifier.startsWith(GRID_BASE_URL)) {
                gridIdentifier = gridIdentifier.substring(GRID_BASE_URL.length());
            } else if (gridIdentifier.startsWith(GRID_BASE_URL_INSTITUTES_ALT)) {
                gridIdentifier = gridIdentifier.substring(GRID_BASE_URL_INSTITUTES_ALT.length());
            } else if (gridIdentifier.startsWith(GRID_BASE_URL_ALT)) {
                gridIdentifier = gridIdentifier.substring(GRID_BASE_URL_ALT.length());
            }
        }
        return gridIdentifier;
    }

    public static boolean duplicates(Assertion a, Assertion b) {
        if (a.getId() != null && a.getId().equals(b.getId())) {
            return false; // both the same record, not two duplicates
        }

        return !different(a.getAffiliationSection(), b.getAffiliationSection()) && !different(a.getDepartmentName(), b.getDepartmentName())
                && !different(a.getRoleTitle(), b.getRoleTitle()) && !different(a.getStartDay(), b.getStartDay()) && !different(a.getStartMonth(), b.getStartMonth())
                && !different(a.getStartYear(), b.getStartYear()) && !different(a.getEndDay(), b.getEndDay()) && !different(a.getEndMonth(), b.getEndMonth())
                && !different(a.getEndYear(), b.getEndYear()) && !different(a.getOrgName(), b.getOrgName()) && !different(a.getOrgCountry(), b.getOrgCountry())
                && !different(a.getOrgCity(), b.getOrgCity()) && !different(a.getOrgRegion(), b.getOrgRegion())
                && !different(a.getDisambiguationSource(), b.getDisambiguationSource()) && !different(a.getDisambiguatedOrgId(), b.getDisambiguatedOrgId())
                && !different(a.getExternalId(), b.getExternalId()) && !different(a.getExternalIdType(), b.getExternalIdType())
                && !different(a.getExternalIdUrl(), b.getExternalIdUrl()) && !different(a.getUrl(), b.getUrl());
    }

    private static boolean different(AffiliationSection affiliationSectionA, AffiliationSection affiliationSectionB) {
        if (affiliationSectionA == null && affiliationSectionB == null) {
            return false;
        }
        return !Objects.equal(affiliationSectionA, affiliationSectionB);
    }

    private static boolean different(String fieldA, String fieldB) {
        if ((fieldA == null || fieldA.isEmpty()) && (fieldB == null || fieldB.isEmpty())) {
            return false;
        }
        return !Objects.equal(fieldA, fieldB);
    }

}
