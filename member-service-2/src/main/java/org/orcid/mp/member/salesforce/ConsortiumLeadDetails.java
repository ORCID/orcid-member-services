package org.orcid.mp.member.salesforce;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConsortiumLeadDetails extends MemberDetails {

    @JsonProperty("consortiumOpportunities")
    private List<ConsortiumMember> consortiumMembers;

    public List<ConsortiumMember> getConsortiumMembers() {
        return consortiumMembers;
    }

    public void setConsortiumMembers(List<ConsortiumMember> consortiumMembers) {
        this.consortiumMembers = consortiumMembers;
    }

}
