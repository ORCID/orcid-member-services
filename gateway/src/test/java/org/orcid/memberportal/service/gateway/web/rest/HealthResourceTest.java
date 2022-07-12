package org.orcid.memberportal.service.gateway.web.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.gateway.service.HealthService;
import org.orcid.memberportal.service.gateway.service.dto.CompositeHealthDTO;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.RouteLocator;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;

public class HealthResourceTest {

    @Mock
    private HealthService healthService;

    @Mock
    private RouteLocator routeLocator;
    
    @InjectMocks
    private HealthResource healthResource;

    @BeforeEach
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        Mockito.when(routeLocator.getRoutes()).thenReturn(getRoutes());
        Mockito.when(healthService.checkGlobalHealth(Mockito.anyList(), Mockito.any(HttpServletRequest.class))).thenReturn(getHealth());
    }


    @Test
    @WithMockUser(username = "test", roles = "ADMIN")
    public void testHealthCheck() throws Exception {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        
        ResponseEntity<CompositeHealthDTO> health = healthResource.healthCheck(request);
        assertThat(health).isNotNull();
        assertThat(health.getStatusCodeValue()).isEqualTo(200);
        assertThat(health.getBody()).isNotNull();
        assertThat(health.getBody().getStatus()).isEqualTo(Status.UP);
        assertThat(health.getBody().getComponents().size()).isEqualTo(2);
        assertThat(health.getBody().getComponents().get("service-1")).isEqualTo(Status.UP);
        assertThat(health.getBody().getComponents().get("service-2")).isEqualTo(Status.UP);
        
        Mockito.verify(routeLocator).getRoutes();
        Mockito.verify(healthService).checkGlobalHealth(Mockito.anyList(), Mockito.any(HttpServletRequest.class));
    }

    private List<Route> getRoutes() {
        return new ArrayList<>();
    }

    private CompositeHealthDTO getHealth() {
        CompositeHealthDTO health = new CompositeHealthDTO();
        health.setStatus(Status.UP);
        health.getComponents().put("service-1", Status.UP);
        health.getComponents().put("service-2", Status.UP);
        return health;
    }
}
