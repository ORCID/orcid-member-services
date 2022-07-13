package org.orcid.memberportal.service.gateway.service.dto;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.health.Status;

/**
 * Bean for holding spring boot actuator health info for multiple components with added details
 */
public final class CompositeHealthDTO {

    private Status status;

    private Map<String, Object> details = new HashMap<>();

    private Map<String, Object> components = new HashMap<>();

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Map<String, Object> getComponents() {
        return components;
    }

    public void setComponents(Map<String, Object> components) {
        this.components = components;
    }

}
