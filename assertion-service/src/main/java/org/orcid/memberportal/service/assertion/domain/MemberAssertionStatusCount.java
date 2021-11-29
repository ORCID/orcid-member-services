package org.orcid.memberportal.service.assertion.domain;

public class MemberAssertionStatusCount {
    
    private String salesforceId;
    
    private String status;
    
    private Integer statusCount;
    
    public MemberAssertionStatusCount(String salesforceId, String status, Integer statusCount) {
        this.salesforceId = salesforceId;
        this.status = status;
        this.statusCount = statusCount;
    }
    
    public MemberAssertionStatusCount() {}

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
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
