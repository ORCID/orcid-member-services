package org.orcid.memberportal.service.assertion.domain.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AssertionUtilsTest {

    @Test
    void testStripGridURL() {
        assertEquals("something", AssertionUtils.stripGridURL("https://www.grid.ac/institutes/something"));
        assertEquals("something", AssertionUtils.stripGridURL("https://www.grid.ac/something"));
        assertEquals("something", AssertionUtils.stripGridURL("https://grid.ac/institutes/something"));
        assertEquals("something", AssertionUtils.stripGridURL("https://grid.ac/something"));
    }

}
