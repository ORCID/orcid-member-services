package org.orcid.mp.assertion.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssertionUtilsTest {

    @Test
    void testStripGridURL() {
        assertEquals("something", AssertionUtils.stripGridURL("https://www.grid.ac/institutes/something"));
        assertEquals("something", AssertionUtils.stripGridURL("https://www.grid.ac/something"));
        assertEquals("something", AssertionUtils.stripGridURL("https://grid.ac/institutes/something"));
        assertEquals("something", AssertionUtils.stripGridURL("https://grid.ac/something"));
    }

}
