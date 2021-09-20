package org.orcid.memberportal.service.assertion.domain.org.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.domain.validation.org.impl.GridOrgValidator;

public class GridOrgValidatorTest {

    @InjectMocks
    private GridOrgValidator validator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testValidId() {
        assertThat(validator.validId("grid.238252.c")).isTrue();
        assertThat(validator.validId(".238252.c")).isFalse();
        assertThat(validator.validId("grid")).isFalse();
    }

}
