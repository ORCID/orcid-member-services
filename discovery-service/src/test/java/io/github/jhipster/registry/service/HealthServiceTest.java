package io.github.jhipster.registry.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.actuate.health.Status;

import io.github.jhipster.registry.client.HealthClient;
import io.github.jhipster.registry.service.dto.SimpleHealthDTO;

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
        SimpleHealthDTO health = new SimpleHealthDTO(Status.UP);
        Mockito.when(healthClient.getHealth(Mockito.eq("url"))).thenReturn(health);
        
        SimpleHealthDTO checkedHealth = healthService.checkHealth("url");
        assertThat(checkedHealth).isNotNull();
        assertThat(checkedHealth.getStatus()).isEqualTo(Status.UP);
        
        Mockito.verify(healthClient).getHealth(Mockito.eq("url"));
    }
    
    @Test
    void testCheckHealthWithError() throws IOException {
        Mockito.when(healthClient.getHealth(Mockito.eq("url"))).thenThrow(new IOException());
        
        Assertions.assertThrows(IOException.class, () -> {
            healthService.checkHealth("url");
        });
    }

}
