package org.orcid.mp.member.salesforce;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConsortiumLeadDetailsWrapper {

    @JsonProperty("member")
    private ConsortiumLeadDetails consortiumLead;

    @JsonProperty("consortiumOpportunities")
    private List<ConsortiumMember> consortiumMembers;

    public ConsortiumLeadDetails getConsortiumLead() {
        return consortiumLead;
    }

    public void setConsortiumLead(ConsortiumLeadDetails consortiumLead) {
        this.consortiumLead = consortiumLead;
    }

    public List<ConsortiumMember> getConsortiumMembers() {
        return consortiumMembers;
    }

    public void setConsortiumMembers(List<ConsortiumMember> consortiumMembers) {
        this.consortiumMembers = consortiumMembers;
    }
}
