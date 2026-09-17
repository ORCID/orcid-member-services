package org.orcid.mp.member.service;

import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
import org.orcid.mp.member.apicreds.ProductionCredentialsApplication;
import org.orcid.mp.member.client.ApiCredentialsServiceClient;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApiCredentialsService {

    @Autowired
    private ApiCredentialsServiceClient apiCredentialsServiceClient;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberService memberService;

    public ApiClientDetails getApiClientDetails(String clientDetailsId) {
        return apiCredentialsServiceClient.getApiClientDetails(clientDetailsId);
    }

    public ApiClientSummaryPage getApiClientsForMember(String memberId) {
        return apiCredentialsServiceClient.getApiClientsForMember(memberId);
    }

    public void applyForApiCredentials(ProductionCredentialsApplication application) {
        User user = userService.getLoggedInUser();
        Member member = memberService.getMember(user.getMemberId()).orElseThrow();

        if (member.getParentSalesforceId() != null) {
            Member consortiumLead = memberService.getMember(member.getParentSalesforceId()).orElseThrow();
            User clOrgOwner = userService.getUsersByMemberId(consortiumLead.getId()).stream()
                    .filter(u -> Boolean.TRUE.equals(u.getMainContact()))
                    .findFirst()
                    .orElse(null);
            application.setConsortiumLeadEmail(clOrgOwner.getEmail());
        }

        application.setRequestedByName(user.getFirstName() + " " + user.getLastName());
        application.setRequestedByEmail(user.getEmail());
        application.setOrgName(member.getClientName());
        mailService.sendApplyForProdCredsEmail(application);
    }
}
