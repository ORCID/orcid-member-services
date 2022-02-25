package org.orcid.memberportal.service.assertion.domain;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "csv_report")
public class CsvReport implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public static final String ASSERTIONS_FOR_EDIT_TYPE = "csv-for-edit";
    
    public static final String PERMISSION_LINKS_TYPE = "permission-links";
    
    public static final String ASSERTIONS_REPORT_TYPE = "assertions-report";
    
    public static final String UNPROCESSED_STATUS = "unprocessed";
    
    public static final String SUCCESS_STATUS = "success";
    
    public static final String FAILURE_STATUS = "failure";

    @Id
    private String id;
    
    @Field("stored_file_id")
    private String storedFileId;
    
    @Field("report_type")
    private String reportType;

    @Field("date_requested")
    private Instant dateRequested;
    
    @Field("date_generated")
    private Instant dateGenerated;
    
    @Field("date_sent")
    private Instant dateSent;
    
    @Field("processing_error")
    private String error;
    
    @Field("original_filename")
    private String originalFilename;
    
    @Field("owner_id")
    private String ownerId;
    
    @Field    
    private String status;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFileId() {
        return storedFileId;
    }

    public void setStoredFileId(String storedFileId) {
        this.storedFileId = storedFileId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public Instant getDateRequested() {
        return dateRequested;
    }

    public void setDateRequested(Instant dateRequested) {
        this.dateRequested = dateRequested;
    }

    public Instant getDateGenerated() {
        return dateGenerated;
    }

    public void setDateGenerated(Instant dateGenerated) {
        this.dateGenerated = dateGenerated;
    }

    public Instant getDateSent() {
        return dateSent;
    }

    public void setDateSent(Instant dateSent) {
        this.dateSent = dateSent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
}
