package org.orcid.memberportal.service.assertion.stats;

import java.util.HashMap;
import java.util.Map;

public class MemberAssertionStats {
    
    private String memberName;
    
    private int totalAssertions = 0;
    
    private Map<String, Integer> statusCounts = new HashMap<>();

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public int getTotalAssertions() {
        return totalAssertions;
    }

    public Map<String, Integer> getStatusCounts() {
        return statusCounts;
    }
    
    public String getStatusCountsString() {
        StringBuilder builder = new StringBuilder();
        for (String key : statusCounts.keySet()) {
            builder.append(key).append(" : ").append(statusCounts.get(key)).append("\n");
        }
        return builder.toString();
    }
    
    public void setStatusCount(String status, Integer count) {
        statusCounts.put(status, count);
        totalAssertions += count;
    }
    
}
