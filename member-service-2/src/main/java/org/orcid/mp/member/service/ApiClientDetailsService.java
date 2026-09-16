package org.orcid.mp.member.service;

import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
import org.orcid.mp.member.client.ApiCredentialsServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiClientDetailsService {

    @Autowired
    private ApiCredentialsServiceClient apiCredentialsServiceClient;

    public ApiClientDetails getApiClientDetails(String clientDetailsId) {
        return apiCredentialsServiceClient.getApiClientDetails(clientDetailsId);
    }

    public ApiClientSummaryPage getApiClientsForMember(String memberId) {
        return apiCredentialsServiceClient.getApiClientsForMember(memberId);
    }
}
