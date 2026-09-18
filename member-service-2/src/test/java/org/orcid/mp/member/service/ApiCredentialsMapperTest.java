package org.orcid.mp.member.service;

import org.junit.jupiter.api.Test;
import org.orcid.mp.member.apicreds.*;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.pojo.Client;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiCredentialsMapperTest {

    private final ApiCredentialsMapper mapper = new ApiCredentialsMapper();

    @Test
    public void testToCreateRequest_mapsFieldsAndDerivesMemberContext() {
        Client client = new Client();
        client.setClientName("My App");
        client.setHomepageUrl("https://example.org");
        client.setDescription("A test client");
        client.setRedirectUris(List.of("https://example.org/callback"));

        Member member = new Member();
        member.setId("507f1f77bcf86cd799439011");
        member.setApiAccessLevel(Member.API_ACCESS_LEVEL_PREMIUM);

        ApiClientDetails dto = mapper.toCreateRequest(client, member);

        assertThat(dto.getName()).isEqualTo("My App");
        assertThat(dto.getWebsite()).isEqualTo("https://example.org");
        assertThat(dto.getDescription()).isEqualTo("A test client");
        assertThat(dto.getMemberId()).isEqualTo("507f1f77bcf86cd799439011");
        assertThat(dto.getMembershipType()).isEqualTo("premium");
        assertThat(dto.getRedirectUris()).extracting(ApiClientRedirectUri::getRedirectUri)
                .containsExactly("https://example.org/callback");
        assertThat(dto.getClientDetailsId()).isNull();
    }

    @Test
    public void testToCreateRequest_defaultsToBasicMembership() {
        Client client = new Client();
        Member member = new Member();
        member.setId("memberId");
        member.setApiAccessLevel(Member.API_ACCESS_LEVEL_BASIC);

        ApiClientDetails dto = mapper.toCreateRequest(client, member);

        assertThat(dto.getMembershipType()).isEqualTo("basic");
    }

    @Test
    public void testToUpdateRequest_setsClientDetailsIdFromClient() {
        Client client = new Client();
        client.setClientId("APP-123");
        Member member = new Member();
        member.setId("memberId");

        ApiClientDetails dto = mapper.toUpdateRequest(client, member);

        assertThat(dto.getClientDetailsId()).isEqualTo("APP-123");
    }

    @Test
    public void testToClient_fromClientDetailsDto_includesSecretAndIsEditable() {
        ApiClientDetails dto = new ApiClientDetails();
        dto.setClientDetailsId("APP-123");
        dto.setName("My App");
        dto.setWebsite("https://example.org");
        dto.setDescription("desc");
        dto.setDecryptedSecret("plaintext-secret");
        Set<ApiClientRedirectUri> uris = new HashSet<>();
        uris.add(new ApiClientRedirectUri("https://example.org/callback"));
        dto.setRedirectUris(uris);

        Client client = mapper.toClient(dto);

        assertThat(client.getClientId()).isEqualTo("APP-123");
        assertThat(client.getClientName()).isEqualTo("My App");
        assertThat(client.getHomepageUrl()).isEqualTo("https://example.org");
        assertThat(client.getDescription()).isEqualTo("desc");
        assertThat(client.getClientSecret()).isEqualTo("plaintext-secret");
        assertThat(client.getRedirectUris()).containsExactly("https://example.org/callback");
        assertThat(client.isEditable()).isTrue();
    }

    @Test
    public void testToClient_fromSummary_activeClientIsEditable() {
        ApiClientDetailsSummary summary = new ApiClientDetailsSummary();
        summary.setClientDetailsId("APP-123");
        summary.setClientName("My App");
        summary.setDeactivatedDate(null);

        Client client = mapper.toClient(summary);

        assertThat(client.getClientId()).isEqualTo("APP-123");
        assertThat(client.getClientName()).isEqualTo("My App");
        assertThat(client.isEditable()).isTrue();
    }

    @Test
    public void testToClient_fromSummary_deactivatedClientIsNotEditable() {
        ApiClientDetailsSummary summary = new ApiClientDetailsSummary();
        summary.setClientDetailsId("APP-123");
        summary.setDeactivatedDate(LocalDateTime.now());

        Client client = mapper.toClient(summary);

        assertThat(client.isEditable()).isFalse();
    }
}
