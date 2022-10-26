package org.orcid.memberportal.service.member.client.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MemberOrgIds {

    @JsonProperty("totalSize")
    private int totalSize;

    @JsonProperty("records")
    private List<MemberOrgId> records;

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public List<MemberOrgId> getRecords() {
        return records;
    }

    public void setRecords(List<MemberOrgId> records) {
        this.records = records;
    }

}
