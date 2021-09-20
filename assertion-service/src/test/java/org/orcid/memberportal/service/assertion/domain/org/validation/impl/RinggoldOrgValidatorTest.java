package org.orcid.memberportal.service.assertion.domain.org.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.domain.validation.org.impl.RinggoldOrgValidator;

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
