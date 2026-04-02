package org.orcid.mp.member.salesforce;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MemberDetailsWrapper {

    @JsonProperty("member")
    private MemberDetails member;

    public MemberDetails getMember() {
        return member;
    }

    public void setMember(MemberDetails member) {
        this.member = member;
    }
}