package org.orcid.memberportal.service.member.client;

import java.util.List;

import org.orcid.memberportal.service.member.client.model.ConsortiumMember;
import org.orcid.memberportal.service.member.client.model.MemberDetails;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MemberDetailsResponseEntity {

    @JsonProperty("member")
    private MemberDetails member;

    @JsonProperty("consortiumOpportunities")
    private List<ConsortiumMember> consortiumMembers;

    public MemberDetails getMember() {
        return member;
    }

    public void setMember(MemberDetails member) {
        this.member = member;
    }

    public List<ConsortiumMember> getConsortiumMembers() {
        return consortiumMembers;
    }

    public void setConsortiumMembers(List<ConsortiumMember> consortiumMembers) {
        this.consortiumMembers = consortiumMembers;
    }

}
