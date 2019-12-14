package org.orcid.domain.enumeration;

public enum AffiliationSection {
    EMPLOYMENT("employment"), EDUCATION("education"), QUALIFICATION("qualification"), INVITED_POSITION("invited-position"), DISTINCTION("distinction"), MEMBERSHIP("membership"), SERVICE("service");
    private final String orcidEndpoint;
    
    private AffiliationSection(String v) {
        this.orcidEndpoint = v;
    }

    public String getOrcidEndpoint() {
        return orcidEndpoint;
    }        
}
