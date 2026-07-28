package org.orcid.mp.assertion.rest;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusResource {

    private final HealthEndpoint healthEndpoint;

    public StatusResource(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        Status status = healthEndpoint.health().getStatus();
        if (Status.UP.equals(status)) {
            return ResponseEntity.ok("UP");
        } else {
            return ResponseEntity.status(503).body("DOWN");
        }
    }
}
