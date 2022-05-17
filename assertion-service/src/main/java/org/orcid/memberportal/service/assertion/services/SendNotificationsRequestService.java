package org.orcid.memberportal.service.assertion.services;

import java.time.Instant;
import java.util.List;

import org.orcid.memberportal.service.assertion.domain.SendNotificationsRequest;
import org.orcid.memberportal.service.assertion.repository.SendNotificationsRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SendNotificationsRequestService {

    private static final Logger LOG = LoggerFactory.getLogger(SendNotificationsRequestService.class);

    @Autowired
    private SendNotificationsRequestRepository sendNotificationsRequestRepository;
    
    public boolean requestInProgress(String salesforceId) {
        return findActiveRequestBySalesforceId(salesforceId) != null;
    }

    public void createSendNotificationsRequest(String userEmail, String salesforceId) {
        if (findActiveRequestBySalesforceId(salesforceId) != null) {
            throw new RuntimeException("Send notifications request already active for " + salesforceId);
        }
        
        SendNotificationsRequest request = new SendNotificationsRequest();
        request.setEmail(userEmail);
        request.setSalesforceId(salesforceId);
        request.setDateRequested(Instant.now());
        sendNotificationsRequestRepository.insert(request);
    }
    
    public void markRequestCompleted(SendNotificationsRequest request) {
        LOG.info("Marking SendNotificationsRequest from user {} (salesforce ID {}) as complete", request.getEmail(), request.getSalesforceId());
        request.setDateCompleted(Instant.now());
        sendNotificationsRequestRepository.save(request);
    }

    public List<SendNotificationsRequest> findActiveRequests() {
        return sendNotificationsRequestRepository.findActiveRequests();
    }

    private SendNotificationsRequest findActiveRequestBySalesforceId(String salesforceId) {
        List<SendNotificationsRequest> requests = sendNotificationsRequestRepository.findActiveRequestBySalesforceId(salesforceId);
        assert requests.size() <= 1;
        return requests.size() == 1 ? requests.get(0) : null;
    }
    
}
