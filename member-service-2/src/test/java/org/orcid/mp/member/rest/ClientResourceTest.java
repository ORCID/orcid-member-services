package org.orcid.mp.member.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.member.client.ApiCredentialsServiceClient;
import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientDetailsSummary;
import org.orcid.mp.member.apicreds.ApiClientPagedResult;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.pojo.Client;
import org.orcid.mp.member.service.ApiCredentialsMapper;
import org.orcid.mp.member.service.MemberService;
import org.orcid.mp.member.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientResourceTest {

    @Mock
    private ApiCredentialsServiceClient apiCredentialsClient;

    @Mock
    private MemberService memberService;

    @Mock
    private UserService userService;

    private final ApiCredentialsMapper apiCredentialsMapper = new ApiCredentialsMapper();

    private ClientResource clientResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        clientResource = new ClientResource();
        setField("apiCredentialsClient", apiCredentialsClient);
        setField("apiCredentialsMapper", apiCredentialsMapper);
        setField("memberService", memberService);
        setField("userService", userService);

        User user = new User();
        user.setMemberId("memberId");
        when(userService.getLoggedInUser()).thenReturn(user);

        Member member = new Member();
        member.setId("memberId");
        member.setApiAccessLevel(Member.API_ACCESS_LEVEL_PREMIUM);
        when(memberService.getMember(eq("memberId"))).thenReturn(Optional.of(member));
    }

    private void setField(String field, Object value) {
        org.springframework.test.util.ReflectionTestUtils.setField(clientResource, field, value);
    }

    @Test
    public void testCreateClient_derivesMemberIdAndMembershipType() {
        Client request = new Client();
        request.setClientName("My App");

        ApiClientDetails created = new ApiClientDetails();
        created.setClientDetailsId("APP-123");
        created.setName("My App");
        created.setDecryptedSecret("plaintext-secret");
        when(apiCredentialsClient.create(any(ApiClientDetails.class))).thenReturn(created);

        ResponseEntity<Client> response = clientResource.createClient(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClientId()).isEqualTo("APP-123");
        assertThat(response.getBody().getClientSecret()).isEqualTo("plaintext-secret");

        org.mockito.ArgumentCaptor<ApiClientDetails> captor = org.mockito.ArgumentCaptor.forClass(ApiClientDetails.class);
        verify(apiCredentialsClient).create(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo("memberId");
        assertThat(captor.getValue().getMembershipType()).isEqualTo("premium");
    }

    @Test
    public void testCreateClient_badRequestFromBackendBecomesBadRequestAlertException() {
        Client request = new Client();
        HttpClientErrorException badRequest = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                "{\"errors\":[\"Client name is required\"]}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        when(apiCredentialsClient.create(any(ApiClientDetails.class))).thenThrow(badRequest);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> clientResource.createClient(request))
                .isInstanceOf(org.orcid.mp.member.error.BadRequestAlertException.class);
    }

    @Test
    public void testUpdateClient_forcesClientIdFromPath() {
        Client request = new Client();
        request.setClientId("different-id");

        ApiClientDetails updated = new ApiClientDetails();
        updated.setClientDetailsId("APP-123");
        when(apiCredentialsClient.update(eq("APP-123"), any(ApiClientDetails.class))).thenReturn(updated);

        ResponseEntity<Client> response = clientResource.updateClient("APP-123", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        org.mockito.ArgumentCaptor<ApiClientDetails> captor = org.mockito.ArgumentCaptor.forClass(ApiClientDetails.class);
        verify(apiCredentialsClient).update(eq("APP-123"), captor.capture());
        assertThat(captor.getValue().getClientDetailsId()).isEqualTo("APP-123");
    }

    @Test
    public void testUpdateClient_notFoundFromBackendReturns404() {
        Client request = new Client();
        HttpClientErrorException notFound = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        when(apiCredentialsClient.update(anyString(), any(ApiClientDetails.class)))
                .thenThrow(notFound);

        ResponseEntity<Client> response = clientResource.updateClient("APP-123", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void testGetClient_mapsDetailsForCurrentMember() {
        ApiClientDetails details = new ApiClientDetails();
        details.setMemberId("memberId");
        details.setClientDetailsId("APP-123");
        details.setName("My App");
        details.setWebsite("https://example.org");
        when(apiCredentialsClient.get("APP-123")).thenReturn(details);

        ResponseEntity<Client> response = clientResource.getClient("APP-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getClientId()).isEqualTo("APP-123");
        assertThat(response.getBody().getClientName()).isEqualTo("My App");
        assertThat(response.getBody().getHomepageUrl()).isEqualTo("https://example.org");
        verify(apiCredentialsClient).get("APP-123");
    }

    @Test
    public void testGetClient_doesNotReturnAnotherMembersDetails() {
        ApiClientDetails details = new ApiClientDetails();
        details.setMemberId("another-member");
        details.setClientDetailsId("APP-123");
        when(apiCredentialsClient.get("APP-123")).thenReturn(details);

        ResponseEntity<Client> response = clientResource.getClient("APP-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void testSearchClients_usesCurrentMemberIdNotClientSupplied() {
        ApiClientDetailsSummary summary = new ApiClientDetailsSummary();
        summary.setClientDetailsId("APP-123");
        summary.setClientName("My App");

        ApiClientPagedResult<ApiClientDetailsSummary> page = new ApiClientPagedResult<>();
        page.setContent(List.of(summary));
        page.setTotalElements(1);
        page.setTotalPages(1);
        page.setNumber(0);
        page.setSize(20);

        when(apiCredentialsClient.search(eq("memberId"), isNull(), eq(0), eq(20), eq("dateCreated,DESC")))
                .thenReturn(page);

        ResponseEntity<ClientResource.PagedClientResult> response =
                clientResource.searchClients(null, 0, 20, "dateCreated,DESC");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getClientId()).isEqualTo("APP-123");
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);

        verify(apiCredentialsClient).search(eq("memberId"), isNull(), eq(0), eq(20), eq("dateCreated,DESC"));
    }
}
