package org.orcid.memberportal.service.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.gateway.client.HealthClient;
import org.orcid.memberportal.service.gateway.service.dto.CompositeHealthDTO;
import org.orcid.memberportal.service.gateway.service.dto.SimpleHealthDTO;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.netflix.zuul.filters.Route;

public class HealthServiceTest {

    @Mock
    private HealthClient healthClient;

    @InjectMocks
    private HealthService healthService;

    @BeforeEach
    public void setUp() throws JAXBException {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testCheckHealth() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        SimpleHealthDTO health = new SimpleHealthDTO(Status.UP);
        Mockito.when(healthClient.getHealth(Mockito.eq("url"))).thenReturn(health);
        
        CompositeHealthDTO checkedHealth = healthService.checkGlobalHealth(getRoutes(), request);
        assertThat(checkedHealth).isNotNull();
        assertThat(checkedHealth.getStatus()).isEqualTo(Status.UP);
        
        Mockito.verify(healthClient).getHealth(Mockito.eq("url"));
    }
    
//    @Test
//    void testCheckHealthWithError() throws IOException {
//        Mockito.when(healthClient.getHealth(Mockito.eq("url"))).thenThrow(new IOException());
//        
//        Assertions.assertThrows(IOException.class, () -> {
//            healthService.checkHealth("url");
//        });
//    }
    
    private List<Route> getRoutes() {
        return new ArrayList<>();
    }

}
