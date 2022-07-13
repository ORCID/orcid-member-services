package org.orcid.memberportal.service.gateway.service.dto;

import org.springframework.boot.actuate.health.Status;

/**
 * Bean for holding spring boot actuator health info, a DTO that can be
 * deserialized by using jackson
 */
public final class SimpleHealthDTO {

    private Status status;

    public SimpleHealthDTO() {
    }

    public SimpleHealthDTO(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

}
