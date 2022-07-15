package org.orcid.memberportal.service.gateway.service;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.orcid.memberportal.service.gateway.client.HealthClient;
import org.orcid.memberportal.service.gateway.service.dto.CompositeHealthDTO;
import org.orcid.memberportal.service.gateway.service.dto.SimpleHealthDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    private static final Logger LOG = LoggerFactory.getLogger(HealthService.class);

    private static final String[] REQUIRED_SERVICES = new String[] { "assertionservice", "gateway", "userservice", "memberservice" };

    @Autowired
    private HealthClient healthClient;

    public CompositeHealthDTO checkGlobalHealth(List<Route> routes, HttpServletRequest request) throws IOException {
        final CompositeHealthDTO globalHealth = new CompositeHealthDTO();
        globalHealth.setStatus(Status.UP);
        routes.forEach(r -> {
            addHealth(globalHealth, r.getId(), r.getPrefix() + "/management/health", request);
        });
        addHealth(globalHealth, "gateway", "/management/health", request);
        
        if (!requiredServicesAllUp(globalHealth)) {
            globalHealth.setStatus(Status.DOWN);
        }
        
        return globalHealth;
    }

    private void addHealth(CompositeHealthDTO globalHealth, String serviceName, String endpoint, HttpServletRequest request) {
        try {
            SimpleHealthDTO health = getHealth(request, endpoint);
            globalHealth.getComponents().put(serviceName, health.getStatus());
            if (!Status.UP.equals(health.getStatus())) {
                globalHealth.setStatus(Status.DOWN);
            }
        } catch (IOException e) {
            LOG.warn("Error finding health for {}", serviceName, e);
            globalHealth.getComponents().put(serviceName, Status.DOWN);
            globalHealth.setStatus(Status.DOWN);
        }
    }

    private SimpleHealthDTO getHealth(HttpServletRequest request, String healthEndpoint) throws IOException {
        URL healthCheckUrl = new URL(request.getScheme(), request.getServerName(), request.getServerPort(), request.getContextPath().concat(healthEndpoint));

        LOG.debug("Hitting health check endpoint {}", healthCheckUrl.toString());
        SimpleHealthDTO health = healthClient.getHealth(healthCheckUrl.toString());
        LOG.debug("Found health for {} - status is {}", healthCheckUrl.toString(), health.getStatus());

        return health;
    }

    private boolean requiredServicesAllUp(CompositeHealthDTO globalHealth) {
        for (String requiredService : REQUIRED_SERVICES) {
            if (!globalHealth.getComponents().containsKey(requiredService) || !Status.UP.equals(globalHealth.getComponents().get(requiredService))) {
                return false;
            }
        }
        return true;
    }
}
