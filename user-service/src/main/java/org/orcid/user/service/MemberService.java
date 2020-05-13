package org.orcid.user.service;

import org.orcid.user.client.MemberServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

	@Autowired
	private MemberServiceClient memberServiceClient;
	
	public boolean memberExistsWithSalesforceId(String salesforceId) {
		ResponseEntity<String> response = memberServiceClient.getMember(salesforceId);
		if (response.getStatusCode().is2xxSuccessful()) {
			return true;
		}
		if (response.getStatusCodeValue() == 404) {
			return false;
		}
		
		throw new RuntimeException("Error contacting member service");
	}
	
}
