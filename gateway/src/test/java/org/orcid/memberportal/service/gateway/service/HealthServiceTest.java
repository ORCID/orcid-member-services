package org.orcid.memberportal.service.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
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
    void testCheckGlobalHealth_notAllHealthy() throws IOException {
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/services/userservice/management/health"))).thenReturn(healthy());
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/services/assertionservice/management/health"))).thenReturn(healthy());
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/services/memberservice/management/health"))).thenReturn(unhealthy());
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/management/health"))).thenReturn(healthy());
        
        CompositeHealthDTO checkedHealth = healthService.checkGlobalHealth(getRoutes(), getMockHttpServletRequest());
        assertThat(checkedHealth).isNotNull();
        assertThat(checkedHealth.getStatus()).isEqualTo(Status.DOWN);
        
        Mockito.verify(healthClient, Mockito.times(4)).getHealth(Mockito.anyString()); // includes call to get gateway health
    }
    
    @Test
    void testCheckGlobalHealth_allHealthy() throws IOException {
        Mockito.when(healthClient.getHealth(Mockito.anyString())).thenReturn(healthy());
        
        CompositeHealthDTO checkedHealth = healthService.checkGlobalHealth(getRoutes(), getMockHttpServletRequest());
        assertThat(checkedHealth).isNotNull();
        assertThat(checkedHealth.getStatus()).isEqualTo(Status.UP);
        
        Mockito.verify(healthClient, Mockito.times(4)).getHealth(Mockito.anyString()); // includes call to get gateway health
    }
    
    @Test
    void testCheckGlobalHealth_WithError() throws IOException {
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/services/userservice/management/health"))).thenReturn(healthy());
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/services/assertionservice/management/health"))).thenReturn(healthy());
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/services/memberservice/management/health"))).thenReturn(healthy());
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/management/health"))).thenThrow(new IOException("overall status should be DOWN"));
        
        CompositeHealthDTO checkedHealth = healthService.checkGlobalHealth(getRoutes(), getMockHttpServletRequest());
        assertThat(checkedHealth).isNotNull();
        assertThat(checkedHealth.getStatus()).isEqualTo(Status.DOWN);
        
        Mockito.verify(healthClient, Mockito.times(4)).getHealth(Mockito.anyString()); // includes call to get gateway health
    }
    
    @Test
    void testCheckGlobalHealth_OneMissing() throws IOException {
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/services/userservice/management/health"))).thenReturn(healthy());
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/services/assertionservice/management/health"))).thenReturn(healthy());
        Mockito.when(healthClient.getHealth(Mockito.eq("http://localhost:8080/management/health"))).thenReturn(healthy());
        
        CompositeHealthDTO checkedHealth = healthService.checkGlobalHealth(getRoutesWithoutMemberService(), getMockHttpServletRequest());
        assertThat(checkedHealth).isNotNull();
        assertThat(checkedHealth.getStatus()).isEqualTo(Status.DOWN);
        
        Mockito.verify(healthClient, Mockito.times(3)).getHealth(Mockito.anyString()); // includes call to get gateway health
    }
    
    private List<Route> getRoutes() {
        Route userService = new Route("userservice", null, null, "/services/userservice", false, null);
        Route memberService = new Route("memberservice", null, null, "/services/memberservice", false, null);
        Route assertionService = new Route("assertionservice", null, null, "/services/assertionservice", false, null);
        return Arrays.asList(userService, memberService, assertionService);
    }
    
    private List<Route> getRoutesWithoutMemberService() {
        Route userService = new Route("userservice", null, null, "/services/userservice", false, null);
        Route assertionService = new Route("assertionservice", null, null, "/services/assertionservice", false, null);
        return Arrays.asList(userService, assertionService);
    }
    
    private HttpServletRequest getMockHttpServletRequest() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        Mockito.when(request.getServerName()).thenReturn("localhost");
        Mockito.when(request.getServerPort()).thenReturn(8080);
        Mockito.when(request.getProtocol()).thenReturn("http");
        Mockito.when(request.getScheme()).thenReturn("http");
        Mockito.when(request.getContextPath()).thenReturn("");
        return request;
    }
    
    private SimpleHealthDTO healthy() {
        return new SimpleHealthDTO(Status.UP);
    }
    
    private SimpleHealthDTO unhealthy() {
        return new SimpleHealthDTO(Status.DOWN);
    }
    
    

}
