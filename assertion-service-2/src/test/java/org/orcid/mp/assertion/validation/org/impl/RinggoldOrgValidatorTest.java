package org.orcid.mp.assertion.validation.org.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

public class RinggoldOrgValidatorTest {

    @InjectMocks
    private RinggoldOrgValidator validator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testValidId() {
        assertThat(validator.validId("12345")).isTrue();
        assertThat(validator.validId("abcde")).isFalse();
    }

}
