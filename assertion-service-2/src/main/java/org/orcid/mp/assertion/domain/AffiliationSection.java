package org.orcid.mp.assertion.domain;

public enum AffiliationSection {
    EMPLOYMENT("employment"), EDUCATION("education"), QUALIFICATION("qualification"), INVITED_POSITION("invited-position"), DISTINCTION("distinction"), MEMBERSHIP(
            "membership"), SERVICE("service");

    private final String value;

    AffiliationSection(String v) {
        this.value = v;
    }

    public String getOrcidEndpoint() {
        return value;
    }
}