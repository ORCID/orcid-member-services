package org.orcid.memberportal.service.member.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MemberContact {
    
    @JsonProperty("Organization__c")
    private String salesforceId;
    
    @JsonProperty("Voting_Contact__c")
    private boolean votingContact;
    
    @JsonProperty("Contact_Curr_Email__c")
    private String email;
    
    @JsonProperty("Name")
    private String name;
    
    @JsonProperty("Member_Org_Role__c")
    private String role;

	public String getSalesforceId() {
		return salesforceId;
	}

	public void setSalesforceId(String salesforceId) {
		this.salesforceId = salesforceId;
	}

	public boolean isVotingContact() {
		return votingContact;
	}

	public void setVotingContact(boolean votingContact) {
		this.votingContact = votingContact;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
    
}
