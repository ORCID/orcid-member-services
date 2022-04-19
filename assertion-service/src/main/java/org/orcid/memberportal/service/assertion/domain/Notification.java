package org.orcid.memberportal.service.assertion.domain;

import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;

public class Notification {
    
    private String orcidId;
    
    private String subject;
    
    private String intro;
    
    private AffiliationSection type;
    
    private String name;
    
    public String getOrcidId() {
        return orcidId;
    }

    public void setOrcidId(String orcidId) {
        this.orcidId = orcidId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public AffiliationSection getType() {
        return type;
    }

    public void setType(AffiliationSection type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
