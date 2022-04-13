package org.orcid.memberportal.service.assertion.domain.utils;

import org.apache.commons.lang3.StringUtils;

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

}
