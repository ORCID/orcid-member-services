package io.github.jhipster.registry.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.URLEncoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.github.jhipster.registry.JHipsterRegistryApp;
import io.github.jhipster.registry.client.HealthClient;
import io.github.jhipster.registry.health.HealthStatus;
import io.github.jhipster.registry.health.Health;
import io.github.jhipster.registry.service.HealthService;

/**
 * Integration tests for the {@link HealthResource} REST controller.
 */
@SpringBootTest(classes = JHipsterRegistryApp.class)
public class HealthResourceIT {

    @Autowired
    private HealthService healthService;
    
    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws IOException {
        HealthClient healthClient = Mockito.mock(HealthClient.class);
        Health healthVM = new Health();
        healthVM.setStatus(HealthStatus.UP);
        Mockito.when(healthClient.getHealth(Mockito.anyString())).thenReturn(healthVM);
        ReflectionTestUtils.setField(healthService, "healthClient", healthClient);
        
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
    }

    @Test
    @WithMockUser(username = "test", roles = "ADMIN")
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health/check/" + URLEncoder.encode("url", "UTF-8")).accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.status").value("UP"));
    }

}
