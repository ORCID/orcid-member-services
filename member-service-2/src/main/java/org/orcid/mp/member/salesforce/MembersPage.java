package org.orcid.mp.member.salesforce;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MembersPage {

    private int totalSize;

    private boolean done;

    private String nextRecordsUrl;

    private List<MemberDetails> records;

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getNextRecordsUrl() {
        return nextRecordsUrl;
    }

    public void setNextRecordsUrl(String nextRecordsUrl) {
        this.nextRecordsUrl = nextRecordsUrl;
    }

    public List<MemberDetails> getRecords() {
        return records;
    }

    public void setRecords(List<MemberDetails> records) {
        this.records = records;
    }
}
