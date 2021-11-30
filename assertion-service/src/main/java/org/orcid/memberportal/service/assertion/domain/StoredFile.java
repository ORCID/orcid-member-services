package org.orcid.memberportal.service.assertion.domain;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "stored_file")
public class StoredFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;
    
    @Field("file_location")
    private String fileLocation;
    
    @Field("file_type")
    private String fileType;

    @Field("date_stored")
    private Instant dateWritten;
    
    @Field("removal_date")
    private Instant removalDate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public void setFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Instant getDateWritten() {
        return dateWritten;
    }

    public void setDateWritten(Instant dateWritten) {
        this.dateWritten = dateWritten;
    }

    public Instant getRemovalDate() {
        return removalDate;
    }

    public void setRemovalDate(Instant removalDate) {
        this.removalDate = removalDate;
    }
    
}
