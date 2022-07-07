package io.github.jhipster.registry.web.rest;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.jhipster.registry.service.EurekaService;
import io.github.jhipster.registry.service.HealthService;
import io.github.jhipster.registry.service.dto.HealthDTO;

/**
 * REST controller for fetching health data for microservice instances
 */
@RestController
@RequestMapping("/api")
public class HealthResource {

    private final Logger LOG = LoggerFactory.getLogger(HealthResource.class);
    
    @Autowired
    private HealthService healthService;
    
    @Autowired
    private EurekaService eurekaService;


    /**
     * GET  /check/<encoded healthcheck url> : get healthcheck details via backend from encoded url supplied
     *
     * @return health check details
     * @throws IOException 
     */
    @GetMapping(path = "/health/{appName}/{serviceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<HealthDTO> healthCheck(@PathVariable("appName") String appName, @PathVariable("serviceId") String serviceId) throws IOException {
        appName = URLDecoder.decode(appName, "UTF-8");
        serviceId = URLDecoder.decode(serviceId, "UTF-8");
        
       
        Map<String, Object> selectedApp = getSelectedApp(appName);
        if (selectedApp == null) {
            LOG.error("Unable to find selected app {} for health check!", appName);
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> selectedInstance = getSelectedInstance(selectedApp, serviceId);
        if (selectedInstance == null) {
            LOG.error("Unable to find selected instance of {} for health check!", appName);
            return ResponseEntity.notFound().build();
        }
        
        String healthCheckUrl = (String) selectedInstance.get("healthCheckUrl");
        try {
            HealthDTO health = healthService.checkHealth(healthCheckUrl);
            return ResponseEntity.ok(health);
        } catch (IOException e) {
            LOG.error("Error checking conducting health check", e);
            throw e;
        }
    }


    private Map<String, Object> getSelectedInstance(Map<String, Object> selectedApp, String serviceId) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> instances = (List<Map<String, Object>>) selectedApp.get("instances");
        for (Map<String, Object> instance : instances) {
            if (serviceId != null && serviceId.equalsIgnoreCase((String) instance.get("instanceId"))) {
                return instance;
            }
        }
        return null;
    }


    private Map<String, Object> getSelectedApp(String appName) {
        List<Map<String, Object>> applications = eurekaService.getApplications();
        for (Map<String, Object> app : applications) {
            if (appName.equalsIgnoreCase((String) app.get("name"))) {
                return app;
            }
        }
        return null;
    }
}
