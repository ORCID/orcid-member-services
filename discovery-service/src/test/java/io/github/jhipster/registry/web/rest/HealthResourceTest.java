package io.github.jhipster.registry.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

import io.github.jhipster.registry.service.EurekaService;
import io.github.jhipster.registry.service.HealthService;
import io.github.jhipster.registry.service.dto.HealthDTO;

public class HealthResourceTest {

    @Mock
    private HealthService healthService;

    @Mock
    private EurekaService eurekaService;

    @InjectMocks
    private HealthResource healthResource;

    @BeforeEach
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        Mockito.when(eurekaService.getApplications()).thenReturn(getApplications());
        Mockito.when(healthService.checkHealth(Mockito.eq("/app-1/some/other/healthcheck/url"))).thenReturn(getHealth());
    }

    @Test
    @WithMockUser(username = "test", roles = "ADMIN")
    public void testHealthCheck() throws Exception {
        ResponseEntity<HealthDTO> health = healthResource.healthCheck("app-1", "instance-2");
        assertThat(health).isNotNull();
        assertThat(health.getStatusCodeValue()).isEqualTo(200);
        assertThat(health.getBody()).isNotNull();
        assertThat(health.getBody().getStatus()).isEqualTo(Status.UP);
        assertThat(health.getBody().getComponents().size()).isEqualTo(1);
        assertThat(health.getBody().getComponents().get(HealthDTO.SERVICE_LABEL)).isEqualTo(Status.UP);
    }

    private List<Map<String, Object>> getApplications() {
        List<Map<String, Object>> apps = new ArrayList<>();

        Map<String, Object> app1 = new HashMap<>();
        List<Map<String, Object>> app1Instances = new ArrayList<>();

        Map<String, Object> app1Instance1 = new HashMap<>();
        app1Instance1.put("instanceId", "instance-1");
        app1Instance1.put("healthCheckUrl", "/app-1/some/healthcheck/url");
        app1Instances.add(app1Instance1);

        Map<String, Object> app1Instance2 = new HashMap<>();
        app1Instance2.put("instanceId", "instance-2");
        app1Instance2.put("healthCheckUrl", "/app-1/some/other/healthcheck/url");
        app1Instances.add(app1Instance2);

        app1.put("name", "app-1");
        app1.put("instances", app1Instances);
        apps.add(app1);

        Map<String, Object> app2 = new HashMap<>();
        List<Map<String, Object>> app2Instances = new ArrayList<>();

        Map<String, Object> app2Instance1 = new HashMap<>();
        app2Instance1.put("instanceId", "app-2-instance-1");
        app2Instance1.put("healthCheckUrl", "/app-2/some/healthcheck/url");
        app1Instances.add(app2Instance1);

        Map<String, Object> app2Instance2 = new HashMap<>();
        app2Instance2.put("instanceId", "app-2-instance-2");
        app2Instance2.put("healthCheckUrl", "/app-2/some/other/healthcheck/url");
        app2Instances.add(app2Instance2);

        app2.put("name", "app-2");
        app2.put("instances", app2Instances);
        apps.add(app1);

        return apps;
    }

    private HealthDTO getHealth() {
        HealthDTO health = new HealthDTO(Status.UP);
        return health;
    }
}
