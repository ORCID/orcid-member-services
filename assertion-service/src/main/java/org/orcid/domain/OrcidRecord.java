package org.orcid.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Document(collection = "orcid_record")
public class OrcidRecord {
    private static final Logger LOG = LoggerFactory.getLogger(OrcidRecord.class);
    
    public static String KEY_TOKEN_ID= "token_id";
    public static String KEY_SALESFORCE_ID= "salesforce_id";
    
    @Id
    private String id;

    @NotNull
    @Pattern(regexp = ".*@.*\\..*")
    @Field("email")
    private String email;

    @Field("orcid")
    private String orcid;

    @Field("tokens")
    @JsonIgnore
    private List<OrcidToken> tokens;

    @Field("last_notified")
    private Instant lastNotified;

    @Field("revoke_notification_sent_date")
    private Instant revokeNotificationSentDate;

    @Field("reminder_notification_sent_date")
    private Instant reminderNotificationSentDate;

    @Field("created")
    private Instant created;

    @Field("modified")
    private Instant modified;

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

    public String getOrcid() {
        return orcid;
    }

    public void setOrcid(String orcid) {
        this.orcid = orcid;
    }

    public List<OrcidToken> getTokens() {
        return tokens;
    }

    public void setTokens(List<OrcidToken> tokens) {
        this.tokens = tokens;
    }

    public Instant getLastNotified() {
        return lastNotified;
    }

    public void setLastNotified(Instant lastNotified) {
        this.lastNotified = lastNotified;
    }

    public Instant getRevokeNotificationSentDate() {
        return revokeNotificationSentDate;
    }

    public void setRevokeNotificationSentDate(Instant revokeNotificationSentDate) {
        this.revokeNotificationSentDate = revokeNotificationSentDate;
    }

    public Instant getReminderNotificationSentDate() {
        return reminderNotificationSentDate;
    }

    public void setReminderNotificationSentDate(Instant reminderNotificationSentDate) {
        this.reminderNotificationSentDate = reminderNotificationSentDate;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getModified() {
        return modified;
    }

    public void setModified(Instant modified) {
        this.modified = modified;
    }
    
    public String getToken(String salesforceId) {
        List<OrcidToken> tokens = this.getTokens();
        if(tokens != null) {
            for(OrcidToken token: tokens)
            {   
                if(StringUtils.equals(token.getSalesforce_id(), salesforceId)) {
                	if(StringUtils.isBlank(token.getToken_id())) {
                		return null;
                	}
                    return token.getToken_id();
                }
            }
        }
        return null;  
    }
    
    public Instant getDeniedDate(String salesforceId) {
        List<OrcidToken> tokens = this.getTokens();
        if(tokens != null) {
            for(OrcidToken token: tokens)
            {   
                if(StringUtils.equals(token.getSalesforce_id(), salesforceId)) {
                	if(token.getDenied_date() == null) {
                		return null;
                	}
                    return token.getDenied_date();
                }
            }
        }
        return null;  
    }
    
    
    public Instant getRevokedDate(String salesforceId) {
        List<OrcidToken> tokens = this.getTokens();
        if(tokens != null) {
            for(OrcidToken token: tokens)
            {   
                if(StringUtils.equals(token.getSalesforce_id(), salesforceId)) {
                	if(token.getRevoked_date() == null) {
                		return null;
                	}
                    return token.getRevoked_date();
                }
            }
        }
        return null;  
    }
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((tokens == null) ? 0 : tokens.hashCode());
        result = prime * result + ((lastNotified == null) ? 0 : lastNotified.hashCode());
        result = prime * result + ((modified == null) ? 0 : modified.hashCode());
        result = prime * result + ((orcid == null) ? 0 : orcid.hashCode());
        result = prime * result + ((reminderNotificationSentDate == null) ? 0 : reminderNotificationSentDate.hashCode());
        result = prime * result + ((revokeNotificationSentDate == null) ? 0 : revokeNotificationSentDate.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OrcidRecord other = (OrcidRecord) obj;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (email == null) {
            if (other.email != null)
                return false;
        } else if (!email.equals(other.email))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (tokens == null) {
            if (other.tokens != null)
                return false;
        } else if (!tokens.equals(other.tokens))
            return false;
        if (lastNotified == null) {
            if (other.lastNotified != null)
                return false;
        } else if (!lastNotified.equals(other.lastNotified))
            return false;
        if (modified == null) {
            if (other.modified != null)
                return false;
        } else if (!modified.equals(other.modified))
            return false;
        if (orcid == null) {
            if (other.orcid != null)
                return false;
        } else if (!orcid.equals(other.orcid))
            return false;
        if (reminderNotificationSentDate == null) {
            if (other.reminderNotificationSentDate != null)
                return false;
        } else if (!reminderNotificationSentDate.equals(other.reminderNotificationSentDate))
            return false;
        if (revokeNotificationSentDate == null) {
            if (other.revokeNotificationSentDate != null)
                return false;
        } else if (!revokeNotificationSentDate.equals(other.revokeNotificationSentDate))
            return false;
        return true;
    }
}
