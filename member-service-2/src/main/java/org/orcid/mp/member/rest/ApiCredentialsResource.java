package org.orcid.mp.member.rest;

import org.orcid.mp.member.apicreds.*;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.service.ApiCredentialsService;
import org.orcid.mp.member.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/apicreds")
public class ApiCredentialsResource {

    private static final Logger LOG = LoggerFactory.getLogger(ApiCredentialsResource.class);

    @Autowired
    private ApiCredentialsService apiClientDetailsService;

    @Autowired
    private UserService userService;

    @GetMapping("/{clientDetailsId}")
    public ResponseEntity<ApiClientDetails> getApiClientDetails(@PathVariable String clientDetailsId) {
        LOG.debug("REST request to get api client details {}", clientDetailsId);

        ApiClientDetails apiClientDetails = apiClientDetailsService.getApiClientDetails(clientDetailsId);
        checkClientAccess(apiClientDetails);
        return ResponseEntity.ok(apiClientDetails);
    }

    @PutMapping("/{clientDetailsId}")
    public ResponseEntity<ApiClientDetails> updateApiClient(@PathVariable String clientDetailsId,
                                                            @RequestBody ApiClientDetails request) {
        LOG.debug("REST request to update api client {}", clientDetailsId);

        ApiClientDetails existing = loadEditableClient(clientDetailsId);
        return ResponseEntity.ok(apiClientDetailsService.updateApiClient(existing, request));
    }

    @GetMapping("/{clientDetailsId}/secret")
    public ResponseEntity<ApiClientSecret> getClientSecret(@PathVariable String clientDetailsId) {
        LOG.debug("REST request to get secret of api client {}", clientDetailsId);

        ApiClientDetails existing = apiClientDetailsService.getApiClientDetails(clientDetailsId);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        checkClientAccess(existing);

        String secret = apiClientDetailsService.getClientSecret(clientDetailsId);
        if (secret == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ApiClientSecret(secret));
    }

    @PostMapping("/{clientDetailsId}/reset-secret")
    public ResponseEntity<ApiClientSecret> resetClientSecret(@PathVariable String clientDetailsId) {
        LOG.debug("REST request to reset secret of api client {}", clientDetailsId);

        ApiClientDetails existing = loadEditableClient(clientDetailsId);
        String secret = apiClientDetailsService.resetClientSecret(existing);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ApiClientSecret(secret));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<ApiClientSummaryPage> getClientsForMember(@PathVariable String memberId) {
        LOG.debug("REST request to get api client details for member id {}", memberId);

        checkClientAccessForMember(memberId);
        ApiClientSummaryPage page = apiClientDetailsService.getApiClientsForMember(memberId);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/apply")
    public ResponseEntity<Void> applyForApiCredentials(@RequestBody ProductionCredentialsApplication application) {
        LOG.debug("REST request to apply for production credentials");
        apiClientDetailsService.applyForApiCredentials(application);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/applyForAMCreds")
    public ResponseEntity<Void> applyForApiCredentials(@RequestBody AMCredentialsApplication application) {
        LOG.debug("REST request to apply for Affiliation Manager credentials");
        apiClientDetailsService.applyForAMCredentials(application);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requestSLClientChange")
    public ResponseEntity<Void> requestSLClientChange(@RequestBody SlClientChangeRequest request) {
        LOG.debug("REST request to change SL client");
        apiClientDetailsService.requestSLClientChange(request);
        return ResponseEntity.ok().build();
    }

    private ApiClientDetails loadEditableClient(String clientDetailsId) {
        ApiClientDetails existing = apiClientDetailsService.getApiClientDetails(clientDetailsId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        checkClientAccess(existing);
        if (existing.isSlClient()) {
            throw new AccessDeniedException("Search & Link clients must be changed via a change request");
        }
        if (existing.getDeactivatedDate() != null) {
            throw new BadRequestAlertException("Deactivated clients cannot be edited");
        }
        return existing;
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