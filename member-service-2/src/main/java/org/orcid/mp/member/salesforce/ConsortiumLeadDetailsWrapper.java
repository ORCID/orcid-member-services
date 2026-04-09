package org.orcid.mp.member.salesforce;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsortiumLeadDetailsWrapper {

    @JsonProperty("member")
    private ConsortiumLeadDetails consortiumLead;

    public ConsortiumLeadDetails getConsortiumLead() {
        return consortiumLead;
    }

    public void setConsortiumLead(ConsortiumLeadDetails consortiumLead) {
        this.consortiumLead = consortiumLead;
    }
}
