package io.github.jhipster.registry.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.jhipster.registry.client.HealthClient;
import io.github.jhipster.registry.service.dto.HealthDTO;

@Service
public class HealthService {

    @Autowired
    private HealthClient healthClient;

    public HealthDTO checkHealth(String healthCheckUrl) throws IOException {
        return healthClient.getHealth(healthCheckUrl);
    }

}
