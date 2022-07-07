package io.github.jhipster.registry.service.dto;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.health.Status;

/**
 * Bean for holding spring boot actuator health info, a DTO that can be
 * deserialized by using jackson
 */
public final class HealthDTO {

    public static final String SERVICE_LABEL = "Service";

    private Status status;

    private Map<String, Object> details = new HashMap<>();

    private Map<String, Object> components = new HashMap<>();

    public HealthDTO() {
    }

    public HealthDTO(Status status, Map<String, Object> components, Map<String, Object> details) {
        this.status = status;
        this.components = components;
        this.details = details;
    }

    public HealthDTO(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        components.put(SERVICE_LABEL, status);
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
