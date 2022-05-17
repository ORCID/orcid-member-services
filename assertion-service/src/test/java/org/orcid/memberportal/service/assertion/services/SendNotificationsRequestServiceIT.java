package org.orcid.memberportal.service.assertion.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orcid.memberportal.service.assertion.AssertionServiceApp;
import org.orcid.memberportal.service.assertion.domain.SendNotificationsRequest;
import org.orcid.memberportal.service.assertion.repository.SendNotificationsRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest(classes = AssertionServiceApp.class)
public class SendNotificationsRequestServiceIT {

    @Autowired
    private SendNotificationsRequestRepository sendNotificationsRequestRepository;

    @Autowired
    private SendNotificationsRequestService sendNotificationsRequestService;

    @BeforeEach
    public void setup() throws IOException {
        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email1", "salesforceId1"));
        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email2", "salesforceId2"));
        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email3", "salesforceId3"));
        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email4", "salesforceId4"));
        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email5", "salesforceId5"));
    }
    
    @AfterEach
    public void tearDown() {
        sendNotificationsRequestRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "any@orcid.org", authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = "password")
    public void testFindActiveRequests() throws Exception {
        List<SendNotificationsRequest> requests = sendNotificationsRequestService.findActiveRequests();
        assertThat(requests.size()).isEqualTo(5);
    }
    
    @Test
    @WithMockUser(username = "any@orcid.org", authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = "password")
    public void testRequestInProgress() throws Exception {
        boolean requestInProgress = sendNotificationsRequestService.requestInProgress("salesforceId1");
        assertThat(requestInProgress).isTrue();
        
        requestInProgress = sendNotificationsRequestService.requestInProgress("salesforceId6");
        assertThat(requestInProgress).isFalse();
    }
    
    @Test
    @WithMockUser(username = "any@orcid.org", authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = "password")
    public void testMarkRequestCompleted() throws Exception {
        List<SendNotificationsRequest> requests = sendNotificationsRequestService.findActiveRequests();
        SendNotificationsRequest first = requests.get(0);
        sendNotificationsRequestService.markRequestCompleted(first);
        requests = sendNotificationsRequestService.findActiveRequests();
        assertThat(requests.size()).isEqualTo(4);
    }
    
    @Test
    @WithMockUser(username = "any@orcid.org", authorities = { "ROLE_ADMIN", "ROLE_USER" }, password = "password")
    public void testCreateSendNotificationsRequest() throws Exception {
        sendNotificationsRequestService.createSendNotificationsRequest("email6", "salesforceId6");
        List<SendNotificationsRequest> requests = sendNotificationsRequestService.findActiveRequests();
        assertThat(requests.size()).isEqualTo(6);
        
        boolean requestInProgress = sendNotificationsRequestService.requestInProgress("salesforceId6");
        assertThat(requestInProgress).isTrue();
    }

    private SendNotificationsRequest getSendNotificationsRequest(String email, String salesforceId) {
        SendNotificationsRequest request = new SendNotificationsRequest();
        request.setEmail(email);
        request.setSalesforceId(salesforceId);
        request.setDateRequested(Instant.now());
        return request;
    }

}
