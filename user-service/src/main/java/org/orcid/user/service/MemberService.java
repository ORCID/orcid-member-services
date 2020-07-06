package org.orcid.user.service;

import feign.FeignException;
import org.orcid.user.client.MemberServiceClient;
import org.orcid.user.service.member.MemberServiceMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

	@Autowired
	private MemberServiceClient memberServiceClient;
	
	public boolean memberExistsWithSalesforceId(String salesforceId) {
	    try {
            ResponseEntity<MemberServiceMember> response = memberServiceClient.getMember(salesforceId);

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }

            if (response.getStatusCode().is4xxClientError()) {
                return false;
            }

        } catch (FeignException ex) {

            HttpStatus status = HttpStatus.resolve(ex.status());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }

            System.out.println(status.value());
            if (status.is2xxSuccessful()) {
                return true;
            }
            if (status.is4xxClientError()) {
                return false;
            }

        }

		throw new RuntimeException("Error contacting member service");
	}
	
	public boolean memberExistsWithSalesforceIdAndAssertionsEnabled(String salesforceId) {
		ResponseEntity<MemberServiceMember> response = memberServiceClient.getMember(salesforceId);
		if (response.getStatusCode().is2xxSuccessful()) {
			return response.getBody().getAssertionServiceEnabled();
		}
		if (response.getStatusCodeValue() == 404) {
			return false;
		}
		
		throw new RuntimeException("Error contacting member service");
	}
	
}
