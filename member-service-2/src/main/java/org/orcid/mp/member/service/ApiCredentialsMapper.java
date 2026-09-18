package org.orcid.mp.member.service;

import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientDetailsSummary;
import org.orcid.mp.member.apicreds.ApiClientRedirectUri;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.pojo.Client;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maps between ui-2's {@link Client} model and the api-credentials-service DTOs.
 * This is the single point where the field-name differences between the two are reconciled.
 */
@Component
public class ApiCredentialsMapper {

    public ApiClientDetails toCreateRequest(Client client, Member member) {
        ApiClientDetails dto = new ApiClientDetails();
        dto.setName(client.getClientName());
        dto.setWebsite(client.getHomepageUrl());
        dto.setDescription(client.getDescription());
        dto.setRedirectUris(toRedirectUriDtos(client.getRedirectUris()));
        dto.setMemberId(member.getId());
        dto.setMembershipType(toMembershipType(member));
        return dto;
    }

    public ApiClientDetails toUpdateRequest(Client client, Member member) {
        ApiClientDetails dto = toCreateRequest(client, member);
        dto.setClientDetailsId(client.getClientId());
        return dto;
    }

    public Client toClient(ApiClientDetails dto) {
        Client client = new Client();
        client.setClientId(dto.getClientDetailsId());
        client.setClientName(dto.getName());
        client.setHomepageUrl(dto.getWebsite());
        client.setDescription(dto.getDescription());
        client.setRedirectUris(toRedirectUriStrings(dto.getRedirectUris()));
        client.setClientSecret(dto.getDecryptedSecret());
        client.setEditable(true);
        return client;
    }

    public Client toClient(ApiClientDetailsSummary summary) {
        Client client = new Client();
        client.setClientId(summary.getClientDetailsId());
        client.setClientName(summary.getClientName());
        client.setEditable(summary.getDeactivatedDate() == null);
        return client;
    }

    private Set<ApiClientRedirectUri> toRedirectUriDtos(List<String> uris) {
        Set<ApiClientRedirectUri> result = new HashSet<>();
        if (uris != null) {
            for (String uri : uris) {
                result.add(new ApiClientRedirectUri(uri));
            }
        }
        return result;
    }

    private List<String> toRedirectUriStrings(Set<ApiClientRedirectUri> uris) {
        List<String> result = new ArrayList<>();
        if (uris != null) {
            for (ApiClientRedirectUri uri : uris) {
                result.add(uri.getRedirectUri());
            }
        }
        return result;
    }

    private String toMembershipType(Member member) {
        return Member.API_ACCESS_LEVEL_PREMIUM.equals(member.getApiAccessLevel())
                ? Member.API_ACCESS_LEVEL_PREMIUM
                : Member.API_ACCESS_LEVEL_BASIC;
    }
}
