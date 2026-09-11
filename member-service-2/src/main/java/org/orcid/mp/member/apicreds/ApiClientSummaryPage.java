package org.orcid.mp.member.apicreds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiClientSummaryPage {

    private List<ApiClientSummary> content;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;
    private int numberOfElements;
    private boolean first;
    private boolean last;
    private boolean empty;

    public List<ApiClientSummary> getContent() { return content; }
    public void setContent(List<ApiClientSummary> content) { this.content = content; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }

    public int getNumberOfElements() { return numberOfElements; }
    public void setNumberOfElements(int numberOfElements) { this.numberOfElements = numberOfElements; }

    public boolean isFirst() { return first; }
    public void setFirst(boolean first) { this.first = first; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }

    public boolean isEmpty() { return empty; }
    public void setEmpty(boolean empty) { this.empty = empty; }
}