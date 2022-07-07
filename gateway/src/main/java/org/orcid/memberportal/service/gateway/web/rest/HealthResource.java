package org.orcid.memberportal.service.gateway.web.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.orcid.memberportal.service.gateway.service.HealthService;
import org.orcid.memberportal.service.gateway.service.dto.CompositeHealthDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private RouteLocator routeLocator;

    /**
     * GET /health/global : global healthcheck hitting all known services
     *
     * @return health check details
     * @throws IOException
     */
    @GetMapping(path = "/health/global", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CompositeHealthDTO> healthCheck(HttpServletRequest request) throws IOException {
        CompositeHealthDTO globalHealth = healthService.checkGlobalHealth(routeLocator.getRoutes(), request);
        return ResponseEntity.ok(globalHealth);
    }

}
