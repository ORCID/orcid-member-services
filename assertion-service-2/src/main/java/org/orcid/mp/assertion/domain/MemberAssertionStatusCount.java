package org.orcid.mp.assertion.domain;

public class MemberAssertionStatusCount {

    private String memberId;

    private String status;

    private Integer statusCount;

    public MemberAssertionStatusCount(String memberId, String status, Integer statusCount) {
        this.memberId = memberId;
        this.status = status;
        this.statusCount = statusCount;
    }

    public MemberAssertionStatusCount() {
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStatusCount() {
        return statusCount;
    }

    public void setStatusCount(Integer statusCount) {
        this.statusCount = statusCount;
    }

}