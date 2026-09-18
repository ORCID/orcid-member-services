package org.orcid.mp.member.rest;

import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.client.ApiCredentialsServiceClient;
import org.orcid.mp.member.apicreds.ApiClientDetailsSummary;
import org.orcid.mp.member.apicreds.ApiClientPagedResult;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.pojo.Client;
import org.orcid.mp.member.service.ApiCredentialsMapper;
import org.orcid.mp.member.service.MemberService;
import org.orcid.mp.member.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Gateway to the external api-credentials-service. Derives {@code memberId} and
 * {@code membershipType} from the authenticated principal's member — never from the client —
 * to prevent cross-member access.
 */
@RestController
@RequestMapping("/api/clients")
public class ClientResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClientResource.class);

    @Autowired
    private ApiCredentialsServiceClient apiCredentialsClient;

    @Autowired
    private ApiCredentialsMapper apiCredentialsMapper;

    @Autowired
    private MemberService memberService;

    @Autowired
    private UserService userService;

    @PostMapping
    @PreAuthorize("hasRole(\"ROLE_ADMIN\") or hasRole(\"ROLE_ORG_OWNER\")")
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        Member member = getCurrentMember();
        ApiClientDetails request = apiCredentialsMapper.toCreateRequest(client, member);
        try {
            ApiClientDetails created = apiCredentialsClient.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(apiCredentialsMapper.toClient(created));
        } catch (HttpClientErrorException.BadRequest e) {
            throw new BadRequestAlertException(extractErrorMessage(e));
        }
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\") or hasRole(\"ROLE_ORG_OWNER\")")
    public ResponseEntity<Client> updateClient(@PathVariable String clientId, @RequestBody Client client) {
        Member member = getCurrentMember();
        client.setClientId(clientId);
        ApiClientDetails request = apiCredentialsMapper.toUpdateRequest(client, member);
        try {
            ApiClientDetails updated = apiCredentialsClient.update(clientId, request);
            return ResponseEntity.ok(apiCredentialsMapper.toClient(updated));
        } catch (HttpClientErrorException.BadRequest e) {
            throw new BadRequestAlertException(extractErrorMessage(e));
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasRole(\"ROLE_ADMIN\") or hasRole(\"ROLE_ORG_OWNER\")")
    public ResponseEntity<Client> getClient(@PathVariable String clientId) {
        Member member = getCurrentMember();
        try {
            ApiClientDetails details = apiCredentialsClient.get(clientId);
            // The route contains a member id for clarity, but the authenticated
            // member remains the authorization source of truth.  Treat a detail
            // response for another member as not found rather than leaking it.
            if (details == null
                    || (details.getMemberId() != null && !details.getMemberId().isBlank()
                            && !member.getId().equals(details.getMemberId()))) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(apiCredentialsMapper.toClient(details));
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.notFound().build();
        } catch (HttpClientErrorException.BadRequest e) {
            throw new BadRequestAlertException(extractErrorMessage(e));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole(\"ROLE_ADMIN\") or hasRole(\"ROLE_ORG_OWNER\")")
    public ResponseEntity<PagedClientResult> searchClients(
            @RequestParam(required = false) List<String> clientType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateCreated,DESC") String sort) {
        Member member = getCurrentMember();
        ApiClientPagedResult<ApiClientDetailsSummary> result = apiCredentialsClient.search(member.getId(), clientType, page, size, sort);
        List<Client> content = result.getContent().stream().map(apiCredentialsMapper::toClient).collect(Collectors.toList());
        PagedClientResult body = new PagedClientResult(content, result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
        return ResponseEntity.ok(body);
    }

    private Member getCurrentMember() {
        String memberId = userService.getLoggedInUser().getMemberId();
        return memberService.getMember(memberId)
                .orElseThrow(() -> new BadRequestAlertException("Member not found for id " + memberId));
    }

    private String extractErrorMessage(HttpClientErrorException.BadRequest e) {
        String body = e.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            return body;
        }
        LOG.warn("api-credentials-service returned 400 with no body", e);
        return "Invalid client details";
    }

    /**
     * Simplified paged response shape for the Angular client.
     */
    public static class PagedClientResult {
        private final List<Client> content;
        private final long totalElements;
        private final int totalPages;
        private final int number;
        private final int size;

        public PagedClientResult(List<Client> content, long totalElements, int totalPages, int number, int size) {
            this.content = content;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
            this.number = number;
            this.size = size;
        }

        public List<Client> getContent() {
            return content;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public int getNumber() {
            return number;
        }

        public int getSize() {
            return size;
        }
    }
}
