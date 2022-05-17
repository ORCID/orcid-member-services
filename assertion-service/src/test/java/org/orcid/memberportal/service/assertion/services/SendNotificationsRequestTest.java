package org.orcid.memberportal.service.assertion.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.memberportal.service.assertion.domain.SendNotificationsRequest;
import org.orcid.memberportal.service.assertion.repository.SendNotificationsRequestRepository;

class SendNotificationsRequestTest {

    @Mock
    private SendNotificationsRequestRepository sendNotificationsRequestRepository;

    @InjectMocks
    private SendNotificationsRequestService sendNotificationsRequestService;

    @Captor
    private ArgumentCaptor<SendNotificationsRequest> requestCaptor;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testCreateSendNotificationsRequest() {
        Mockito.when(sendNotificationsRequestRepository.findActiveRequestBySalesforceId(Mockito.eq("somethingElse"))).thenReturn(new ArrayList<>());
        Mockito.when(sendNotificationsRequestRepository.insert(Mockito.any(SendNotificationsRequest.class))).thenReturn(null);
        sendNotificationsRequestService.createSendNotificationsRequest("email", "salesforceId");
        Mockito.verify(sendNotificationsRequestRepository).insert(requestCaptor.capture());

        SendNotificationsRequest captured = requestCaptor.getValue();
        assertThat(captured.getDateRequested()).isNotNull();
        assertThat(captured.getEmail()).isEqualTo("email");
        assertThat(captured.getSalesforceId()).isEqualTo("salesforceId");
    }
    
    @Test
    void testCreateSendNotificationsRequest_requestInProgress() {
        Mockito.when(sendNotificationsRequestRepository.findActiveRequestBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(getListOfOneRequest());
        Assertions.assertThrows(RuntimeException.class, () -> {
            sendNotificationsRequestService.createSendNotificationsRequest("email", "salesforceId");
        });
    }

    @Test
    void testRequestInProgress() {
        Mockito.when(sendNotificationsRequestRepository.findActiveRequestBySalesforceId(Mockito.eq("salesforceId"))).thenReturn(getListOfOneRequest());
        Mockito.when(sendNotificationsRequestRepository.findActiveRequestBySalesforceId(Mockito.eq("somethingElse"))).thenReturn(new ArrayList<>());
        
        boolean inProgress = sendNotificationsRequestService.requestInProgress("salesforceId");
        assertThat(inProgress).isEqualTo(true);
        
        inProgress = sendNotificationsRequestService.requestInProgress("somethingElse");
        assertThat(inProgress).isEqualTo(false);
    }
    
    @Test
    void testMarkRequestCompleted() {
        Mockito.when(sendNotificationsRequestRepository.save(Mockito.any(SendNotificationsRequest.class))).thenReturn(null);
        
        SendNotificationsRequest request = getRequest();
        sendNotificationsRequestService.markRequestCompleted(request);
        
        Mockito.verify(sendNotificationsRequestRepository).save(requestCaptor.capture());
        
        SendNotificationsRequest captured = requestCaptor.getValue();
        assertThat(captured.getDateCompleted()).isNotNull();
    }
    
    @Test
    void testFindActiveRequests() {
        Mockito.when(sendNotificationsRequestRepository.findActiveRequests()).thenReturn(getListOfManyRequests());
        List<SendNotificationsRequest> activeRequests = sendNotificationsRequestService.findActiveRequests();
        assertThat(activeRequests).isNotNull();
        assertThat(activeRequests.size()).isEqualTo(4);
    }

    private List<SendNotificationsRequest> getListOfManyRequests() {
        return Arrays.asList(getRequest(), getRequest(), getRequest(), getRequest());
    }

    private List<SendNotificationsRequest> getListOfOneRequest() {
        return Arrays.asList(getRequest());
    }
    
    private SendNotificationsRequest getRequest() {
        SendNotificationsRequest request = new SendNotificationsRequest();
        request.setEmail("email");
        request.setSalesforceId("salesforceId");
        request.setDateRequested(Instant.now());
        return request;
    }

}
