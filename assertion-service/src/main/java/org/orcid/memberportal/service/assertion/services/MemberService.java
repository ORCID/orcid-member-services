package org.orcid.memberportal.service.assertion.services;

import org.orcid.memberportal.service.assertion.client.MemberServiceClient;
import org.orcid.memberportal.service.assertion.domain.AssertionServiceMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class MemberService {

    @Autowired
    private MemberServiceClient memberServiceClient;

    public String getMemberName(String salesforceId) {
        ResponseEntity<AssertionServiceMember> response = memberServiceClient.getMember(salesforceId);
        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody().getClientName();
        }
        if (response.getStatusCodeValue() == 404) {
            return null;
        }
        throw new RuntimeException("Error contacting member service");
    }

    public void updateMemberDefaultLanguage(String salesforceId, String language) {
        ResponseEntity<Void> response = memberServiceClient.updateMemberDefaultLanguage(salesforceId, language);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Error updating member default language");
        }
    }

}
