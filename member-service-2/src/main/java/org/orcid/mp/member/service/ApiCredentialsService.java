package org.orcid.mp.member.service;

import org.orcid.mp.member.apicreds.*;
import org.orcid.mp.member.client.ApiCredentialsServiceClient;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.salesforce.MemberContact;
import org.orcid.mp.member.salesforce.MemberContacts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class ApiCredentialsService {

    private static final Logger LOG = LoggerFactory.getLogger(ApiCredentialsService.class);

    private static final Set<String> CLIENT_UPDATE_CONTACT_ROLES =
            Set.of(SalesforceService.MAIN_CONTACT_ROLE, SalesforceService.TECHNICAL_CONTACT_ROLE);

    @Autowired
    private ApiCredentialsServiceClient apiCredentialsServiceClient;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private SalesforceService salesforceService;

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

    public void applyForAMCredentials(AMCredentialsApplication application) {
        User user = userService.getLoggedInUser();
        Member member = memberService.getMember(user.getMemberId()).orElseThrow();

        application.setRequestedByName(user.getFirstName() + " " + user.getLastName());
        application.setRequestedByEmail(user.getEmail());
        // the member can fill in the org name when it is missing on the member record
        if (member.getClientName() != null && !member.getClientName().isBlank()) {
            application.setOrgName(member.getClientName());
        }
        mailService.applyForAMCreds(application);
    }

    public void requestSLClientChange(SlClientChangeRequest slClientChangeRequest) {
        User user = userService.getLoggedInUser();
        Member member = memberService.getMember(user.getMemberId()).orElseThrow();
        slClientChangeRequest.setRequestedByName(user.getFirstName() + " " + user.getLastName());
        slClientChangeRequest.setRequestedByEmail(user.getEmail());
        slClientChangeRequest.setOrgName(member.getClientName());
        mailService.requestSLClientChange(slClientChangeRequest);
    }

    public String getClientSecret(String clientDetailsId) {
        return apiCredentialsServiceClient.getClientSecret(clientDetailsId);
    }

    public ApiClientDetails updateApiClient(ApiClientDetails existing, ApiClientDetails request) {
        String previousWebsite = existing.getWebsite();
        String previousDescription = existing.getDescription();
        List<String> previousUris = redirectUriValues(existing.getRedirectUris());

        existing.setWebsite(request.getWebsite());
        existing.setDescription(request.getDescription());
        existing.setRedirectUris(mergeRedirectUris(existing.getRedirectUris(), request.getRedirectUris()));
        existing.setErrors(new ArrayList<>());

        ApiClientDetails updated = apiCredentialsServiceClient.update(existing.getClientDetailsId(), existing);
        ApiClientDetails result = updated != null ? updated : existing;

        ApiClientUpdateNotification notification = newNotification(existing.getClientDetailsId(),
                result.getClientName() != null ? result.getClientName() : existing.getClientName());
        if (!Objects.equals(normalize(previousWebsite), normalize(result.getWebsite()))) {
            notification.setHomepageUrl(result.getWebsite());
        }
        if (!Objects.equals(normalize(previousDescription), normalize(result.getDescription()))) {
            notification.setDescription(result.getDescription());
        }
        List<String> currentUris = redirectUriValues(result.getRedirectUris());
        notification.setAddedRedirectUris(currentUris.stream().filter(uri -> !previousUris.contains(uri)).toList());
        notification.setRemovedRedirectUris(previousUris.stream().filter(uri -> !currentUris.contains(uri)).toList());

        if (notification.hasChanges()) {
            sendClientUpdatedEmail(notification, existing.getMemberId());
        }
        return updated;
    }

    public String resetClientSecret(ApiClientDetails existing) {
        if (!apiCredentialsServiceClient.resetClientSecret(existing.getClientDetailsId())) {
            throw new IllegalStateException("Client secret reset failed for " + existing.getClientDetailsId());
        }
        String secret = apiCredentialsServiceClient.getClientSecret(existing.getClientDetailsId());

        ApiClientUpdateNotification notification = newNotification(existing.getClientDetailsId(), existing.getClientName());
        notification.setSecretReset(true);
        sendClientUpdatedEmail(notification, existing.getMemberId());
        return secret;
    }

    // Keeps type/scope of URIs that already exist, new URIs get the default type
    private Set<ApiClientRedirectUri> mergeRedirectUris(Set<ApiClientRedirectUri> current, Set<ApiClientRedirectUri> requested) {
        Map<String, ApiClientRedirectUri> currentByUri = new HashMap<>();
        if (current != null) {
            current.forEach(uri -> currentByUri.put(uri.getRedirectUri(), uri));
        }

        Set<ApiClientRedirectUri> merged = new LinkedHashSet<>();
        if (requested == null) {
            return merged;
        }

        Set<String> seen = new HashSet<>();
        for (ApiClientRedirectUri uri : requested) {
            String value = uri.getRedirectUri() == null ? "" : uri.getRedirectUri().trim();
            if (value.isEmpty() || !seen.add(value)) {
                continue;
            }
            ApiClientRedirectUri kept = currentByUri.get(value);
            if (kept == null) {
                kept = new ApiClientRedirectUri();
                kept.setRedirectUri(value);
                kept.setRedirectUriType("default");
            }
            kept.getErrors().clear();
            merged.add(kept);
        }
        return merged;
    }

    private ApiClientUpdateNotification newNotification(String clientId, String clientName) {
        ApiClientUpdateNotification notification = new ApiClientUpdateNotification();
        notification.setClientId(clientId);
        notification.setClientName(clientName);
        return notification;
    }

    private void sendClientUpdatedEmail(ApiClientUpdateNotification notification, String clientMemberId) {
        notification.setRecipients(findClientUpdateRecipients(clientMemberId));
        mailService.sendClientUpdatedEmail(notification);
    }

    // Main + technical Salesforce contacts, org owners and users who can manage API credentials
    private List<String> findClientUpdateRecipients(String clientMemberId) {
        Map<String, String> emails = new LinkedHashMap<>();
        Optional<Member> member = clientMemberId != null ? memberService.getMember(clientMemberId) : Optional.empty();

        if (member.isPresent()) {
            String salesforceId = member.get().getSalesforceId();
            if (salesforceId != null) {
                try {
                    MemberContacts contacts = salesforceService.getMemberContacts(salesforceId);
                    if (contacts != null && contacts.getRecords() != null) {
                        contacts.getRecords().stream()
                                .filter(contact -> CLIENT_UPDATE_CONTACT_ROLES.contains(contact.getRole()))
                                .map(MemberContact::getEmail)
                                .forEach(email -> addEmail(emails, email));
                    }
                } catch (Exception e) {
                    LOG.warn("Unable to load Salesforce contacts for member {}", salesforceId, e);
                }
            }

            try {
                List<User> users = userService.getUsersByMemberId(member.get().getId());
                if (users != null) {
                    users.stream()
                            .filter(u -> Boolean.TRUE.equals(u.getMainContact()) || Boolean.TRUE.equals(u.getManageApiCredsEnabled()))
                            .map(User::getEmail)
                            .forEach(email -> addEmail(emails, email));
                }
            } catch (Exception e) {
                LOG.warn("Unable to load users for member {}", member.get().getId(), e);
            }
        } else {
            LOG.warn("Member {} not found when sending client update email", clientMemberId);
        }

        if (emails.isEmpty()) {
            addEmail(emails, userService.getLoggedInUser().getEmail());
        }
        return new ArrayList<>(emails.values());
    }

    private void addEmail(Map<String, String> emails, String email) {
        if (email != null && !email.isBlank()) {
            emails.putIfAbsent(email.trim().toLowerCase(Locale.ROOT), email.trim());
        }
    }

    private List<String> redirectUriValues(Set<ApiClientRedirectUri> uris) {
        return uris == null ? List.of() : uris.stream().map(ApiClientRedirectUri::getRedirectUri).filter(Objects::nonNull).toList();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
