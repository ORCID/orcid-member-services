package org.orcid.mp.member.rest;

import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.service.ApiClientDetailsService;
import org.orcid.mp.member.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apicreds")
public class ApiCredentialsResource {

    private static final Logger LOG = LoggerFactory.getLogger(ApiCredentialsResource.class);

    @Autowired
    private ApiClientDetailsService apiClientDetailsService;

    @Autowired
    private UserService userService;

    @GetMapping("/{clientDetailsId}")
    public ResponseEntity<ApiClientDetails> getApiClientDetails(@PathVariable String clientDetailsId) {
        LOG.debug("REST request to get api client details {}", clientDetailsId);

        ApiClientDetails apiClientDetails = apiClientDetailsService.getApiClientDetails(clientDetailsId);
        checkClientAccess(apiClientDetails);
        return ResponseEntity.ok(apiClientDetails);
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiClientSummaryPage> getClientsForMember(@PathVariable String memberId) {
        LOG.debug("REST request to get api client details for member id {}", memberId);

        ApiClientSummaryPage page = apiClientDetailsService.getApiClientsForMember(memberId);
        checkClientAccessForMember(memberId);
        return ResponseEntity.ok(page);
    }

    private void checkClientAccessForMember(String memberId) {
        User user = userService.getLoggedInUser();
        if (!user.isAdmin() && !user.getMemberId().equals(memberId)) {
            throw new AccessDeniedException("User " + user.getEmail() + " can't access client details of " + memberId);
        }
    }

    private void checkClientAccess(ApiClientDetails apiClientDetails) {
        checkClientAccessForMember(apiClientDetails.getMemberId());
    }
}