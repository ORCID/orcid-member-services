package org.orcid.memberportal.service.member.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MemberContacts {

	@JsonProperty("totalSize")
	private int totalSize;

	@JsonProperty("records")
	private List<MemberContact> records;

	public int getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(int totalSize) {
		this.totalSize = totalSize;
	}

	public List<MemberContact> getRecords() {
		return records;
	}

	public void setRecords(List<MemberContact> records) {
		this.records = records;
	}

}
