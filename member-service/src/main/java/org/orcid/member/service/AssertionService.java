package org.orcid.member.service;

import org.orcid.member.client.AssertionServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AssertionService {
	
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

}
