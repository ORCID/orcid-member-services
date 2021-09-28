package org.orcid.memberportal.service.assertion.domain.org.validation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.domain.validation.org.impl.RorOrgValidator;

public class RorOrgValidatorTest {

    @InjectMocks
    private RorOrgValidator validator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testValidId() {
        assertThat(validator.validId("03yrm5c26")).isTrue();
        assertThat(validator.validId("13yrm5c26")).isFalse();
        assertThat(validator.validId("03yrm5c2d")).isFalse();
        assertThat(validator.validId("https://ror.org/03yrm5c26")).isTrue();
        assertThat(validator.validId("https://ror.org/03yrm5c2d")).isFalse();
        assertThat(validator.validId("https://ror.org/03yrd")).isFalse();
        assertThat(validator.validId("03yrd")).isFalse();
    }

}
