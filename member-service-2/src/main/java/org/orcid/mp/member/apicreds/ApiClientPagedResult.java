package org.orcid.mp.member.apicreds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Minimal mirror of the default Jackson serialization of a Spring Data {@code Page}
 * (content, totalElements, totalPages, number, size), as returned by the
 * api-credentials-service search endpoint.
 * <p>
 * Since Spring Data 3.3, {@code Page} is by default serialized with the pagination
 * metadata (size, number, totalElements, totalPages) nested under a {@code "page"}
 * object instead of as top-level fields
 * (see {@code spring.data.web.pageable.serialization-mode=VIA_DTO}). This class
 * supports both the legacy flat format and the newer nested {@code "page"} format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiClientPagedResult<T> {

    private List<T> content;

    private long totalElements;

    private int totalPages;

    private int number;

    private int size;

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @JsonProperty("page")
    private void unpackPageMetadata(PageMetadata page) {
        if (page != null) {
            this.totalElements = page.getTotalElements();
            this.totalPages = page.getTotalPages();
            this.number = page.getNumber();
            this.size = page.getSize();
        }
    }

    /**
     * Mirrors the {@code "page"} object introduced by Spring Data's
     * {@code VIA_DTO} pagination serialization mode.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PageMetadata {

        private int size;

        private int number;

        private long totalElements;

        private int totalPages;

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public void setTotalElements(long totalElements) {
            this.totalElements = totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }
    }
}
