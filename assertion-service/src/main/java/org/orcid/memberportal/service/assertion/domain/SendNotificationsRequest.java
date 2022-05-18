package org.orcid.memberportal.service.assertion.domain;

import java.io.Serializable;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "send_notifications_request")
public class SendNotificationsRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    private String id;
    
    @Field("email")
    private String email;
    
    @Field("salesforce_id")
    private String salesforceId;
    
    @Field("date_requested")
    private Instant dateRequested;
    
    @Field("date_completed")
    private Instant dateCompleted;
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getDateRequested() {
        return dateRequested;
    }

    public void setDateRequested(Instant dateRequested) {
        this.dateRequested = dateRequested;
    }

    public Instant getDateCompleted() {
        return dateCompleted;
    }

    public void setDateCompleted(Instant dateCompleted) {
        this.dateCompleted = dateCompleted;
    }

    public String getSalesforceId() {
        return salesforceId;
    }

    public void setSalesforceId(String salesforceId) {
        this.salesforceId = salesforceId;
    }
}
