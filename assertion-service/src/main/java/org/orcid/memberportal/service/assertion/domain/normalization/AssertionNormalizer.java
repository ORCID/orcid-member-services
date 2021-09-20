package org.orcid.memberportal.service.assertion.domain.normalization;

import org.orcid.memberportal.service.assertion.domain.Assertion;

public interface AssertionNormalizer {

    Assertion normalize(Assertion assertion);
    
}
