package org.orcid.member.service;

import org.orcid.member.client.AssertionServiceClient;
import org.orcid.member.web.rest.errors.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AssertionService {
    private static final Logger LOG = LoggerFactory.getLogger(AssertionService.class);

    @Autowired
    private AssertionServiceClient assertionServiceClient;

    public String getOwnerIdForOrcidUser(String encryptedEmail) {
        ResponseEntity<String> response = assertionServiceClient.getOwnerOfUser(encryptedEmail);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            return null;
        }
    }

    public void deleteAssertionsForSalesforceIn(String salesforceId) {
        try {
            ResponseEntity<String> response = assertionServiceClient.deleteAssertionsForSalesforceId(salesforceId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                LOG.warn("Error deleting assertions for  salesforceId {}, response code {}", salesforceId, response.getStatusCodeValue());
                throw new BadRequestAlertException("Unable to delete assertions for salesforceId " + salesforceId, "member", "deleteAssertionsForSalesforceId.string");
            }
        } catch (Exception e) {
            LOG.error("Error when trying to delete assertions for salesforceId: " + salesforceId, e);
            throw new BadRequestAlertException("Unable to delete assertions for salesforceId " + salesforceId, "member", "deleteAssertionsForSalesforceId.string");
        }
    }

    public void updateAssertionsSalesforceId(String salesforceId, String newSalesforceId) {
        try {
            ResponseEntity<String> response = assertionServiceClient.updateAssertionsSalesforceId(salesforceId, newSalesforceId);
            if (!response.getStatusCode().is2xxSuccessful()) {
                LOG.warn("Error updating assertions for  salesforceId {} to the new salesforceId {}, response code {}", salesforceId, newSalesforceId,
                        response.getStatusCodeValue());
                throw new BadRequestAlertException("Unable to update assertions for salesforceId " + salesforceId + " to the new salesforceId " + newSalesforceId,
                        "member", "updateAssertionsSalesforceId.string");
            }
        } catch (Exception e) {
            LOG.error("Error updating assertions for  salesforceId {} to the new salesforceId {}", salesforceId, newSalesforceId, e);
            throw new BadRequestAlertException("Unable to update assertions for salesforceId " + salesforceId + " to the new salesforceId " + newSalesforceId, "member",
                    "updateAssertionsSalesforceId.string");
        }
    }

}
