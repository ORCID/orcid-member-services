package io.github.jhipster.registry.web.rest;

import java.io.IOException;
import java.net.URLDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.jhipster.registry.health.Health;
import io.github.jhipster.registry.service.HealthService;

/**
 * REST controller for fetching health data for microservice instances
 */
@RestController
@RequestMapping("/health")
public class HealthResource {

    private final Logger LOG = LoggerFactory.getLogger(HealthResource.class);
    
    @Autowired
    private HealthService healthService;


    /**
     * GET  /check/<encoded healthcheck url> : get healthcheck details via backend from encoded url supplied
     *
     * @return health check details
     * @throws IOException 
     */
    @GetMapping("/check/{healthCheckUrl}")
    public ResponseEntity<Health> healthCheck(@PathVariable("healthCheckUrl") String healthCheckUrl) throws IOException {
        try {
            String decodedUrl = URLDecoder.decode(healthCheckUrl, "UTF-8");
            Health health = healthService.checkHealth(decodedUrl);
            return ResponseEntity.ok(health);
        } catch (IOException e) {
            LOG.error("Error checking conducting health check", e);
            throw e;
        }
    }
}
