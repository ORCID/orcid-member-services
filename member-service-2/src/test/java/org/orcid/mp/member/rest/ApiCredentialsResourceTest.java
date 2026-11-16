package org.orcid.mp.member.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSecret;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
import org.orcid.mp.member.apicreds.ProductionCredentialsApplication;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.BadRequestAlertException;
import org.orcid.mp.member.service.ApiCredentialsService;
import org.orcid.mp.member.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiCredentialsResourceTest {

    @Mock
    private ApiCredentialsService apiClientDetailsService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ApiCredentialsResource apiCredentialsResource;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setEmail("test@example.com");
    }

    @Test
    void getApiClientDetails_WhenUserIsAdmin_ShouldReturn200() {
        // Arrange
        String clientId = "APP-123";
        mockUser.setAdmin(true);
        when(userService.getLoggedInUser()).thenReturn(mockUser);

        ApiClientDetails mockDetails = new ApiClientDetails();
        mockDetails.setClientDetailsId(clientId);
        mockDetails.setMemberId("MEMBER-ANY");
        when(apiClientDetailsService.getApiClientDetails(clientId)).thenReturn(mockDetails);

        // Act
        ResponseEntity<ApiClientDetails> response = apiCredentialsResource.getApiClientDetails(clientId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mockDetails);
        verify(apiClientDetailsService).getApiClientDetails(clientId);
    }

    @Test
    void getApiClientDetails_WhenUserBelongsToMember_ShouldReturn200() {
        // Arrange
        String clientId = "APP-123";
        mockUser.setAdmin(false);
        mockUser.setMemberId("MEMBER-MATCH");
        when(userService.getLoggedInUser()).thenReturn(mockUser);

        ApiClientDetails mockDetails = new ApiClientDetails();
        mockDetails.setClientDetailsId(clientId);
        mockDetails.setMemberId("MEMBER-MATCH");
        when(apiClientDetailsService.getApiClientDetails(clientId)).thenReturn(mockDetails);

        // Act
        ResponseEntity<ApiClientDetails> response = apiCredentialsResource.getApiClientDetails(clientId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mockDetails);
    }

    @Test
    void getApiClientDetails_WhenUserDoesNotBelongToMember_ShouldThrowAccessDenied() {
        // Arrange
        String clientId = "APP-123";
        mockUser.setAdmin(false);
        mockUser.setMemberId("MEMBER-OTHER");
        when(userService.getLoggedInUser()).thenReturn(mockUser);

        ApiClientDetails mockDetails = new ApiClientDetails();
        mockDetails.setMemberId("MEMBER-TARGET");
        when(apiClientDetailsService.getApiClientDetails(clientId)).thenReturn(mockDetails);

        // Act & Assert
        assertThatThrownBy(() -> apiCredentialsResource.getApiClientDetails(clientId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User test@example.com can't access client details of MEMBER-TARGET");
    }

    @Test
    void getClientsForMember_WhenUserIsAdmin_ShouldReturn200() {
        // Arrange
        String memberId = "MEMBER-123";
        mockUser.setAdmin(true);
        when(userService.getLoggedInUser()).thenReturn(mockUser);

        ApiClientSummaryPage mockPage = new ApiClientSummaryPage();
        mockPage.setContent(Collections.emptyList());
        when(apiClientDetailsService.getApiClientsForMember(memberId)).thenReturn(mockPage);

        // Act
        ResponseEntity<ApiClientSummaryPage> response = apiCredentialsResource.getClientsForMember(memberId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mockPage);
        verify(apiClientDetailsService).getApiClientsForMember(memberId);
    }

    @Test
    void getClientsForMember_WhenUserBelongsToMember_ShouldReturn200() {
        // Arrange
        String memberId = "MEMBER-MATCH";
        mockUser.setAdmin(false);
        mockUser.setMemberId("MEMBER-MATCH");
        when(userService.getLoggedInUser()).thenReturn(mockUser);

        ApiClientSummaryPage mockPage = new ApiClientSummaryPage();
        when(apiClientDetailsService.getApiClientsForMember(memberId)).thenReturn(mockPage);

        // Act
        ResponseEntity<ApiClientSummaryPage> response = apiCredentialsResource.getClientsForMember(memberId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(mockPage);
    }

    @Test
    void getClientsForMember_WhenUserDoesNotBelongToMember_ShouldThrowAccessDenied() {
        // Arrange
        String memberId = "MEMBER-TARGET";
        mockUser.setAdmin(false);
        mockUser.setMemberId("MEMBER-OTHER");
        when(userService.getLoggedInUser()).thenReturn(mockUser);

        // Act & Assert
        assertThatThrownBy(() -> apiCredentialsResource.getClientsForMember(memberId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User test@example.com can't access client details of MEMBER-TARGET");
    }

    @Test
    void applyForApiCredentials_ShouldReturn200AndDelegateToService() {
        // Arrange
        ProductionCredentialsApplication application = new ProductionCredentialsApplication();
        application.setNotes("some notes");

        // Act
        ResponseEntity<Void> response = apiCredentialsResource.applyForApiCredentials(application);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(apiClientDetailsService).applyForApiCredentials(application);
    }

    @Test
    void updateApiClient_WhenUserBelongsToMember_ShouldDelegateToService() {
        mockUser.setAdmin(false);
        mockUser.setMemberId("MEMBER-MATCH");
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        ApiClientDetails existing = client("MEMBER-MATCH");
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(existing);
        ApiClientDetails request = new ApiClientDetails();
        ApiClientDetails updated = new ApiClientDetails();
        when(apiClientDetailsService.updateApiClient(existing, request)).thenReturn(updated);

        ResponseEntity<ApiClientDetails> response = apiCredentialsResource.updateApiClient("APP-123", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(updated);
    }

    @Test
    void updateApiClient_WhenUserIsAdmin_ShouldDelegateToService() {
        mockUser.setAdmin(true);
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        ApiClientDetails existing = client("ANY-MEMBER");
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(existing);
        when(apiClientDetailsService.updateApiClient(any(), any())).thenReturn(existing);

        assertThat(apiCredentialsResource.updateApiClient("APP-123", new ApiClientDetails()).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateApiClient_WhenUserDoesNotBelongToMember_ShouldThrowAccessDenied() {
        mockUser.setAdmin(false);
        mockUser.setMemberId("MEMBER-OTHER");
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(client("MEMBER-TARGET"));

        assertThatThrownBy(() -> apiCredentialsResource.updateApiClient("APP-123", new ApiClientDetails()))
                .isInstanceOf(AccessDeniedException.class);
        verify(apiClientDetailsService, never()).updateApiClient(any(), any());
    }

    @Test
    void updateApiClient_WhenClientIsSl_ShouldThrowAccessDenied() {
        mockUser.setAdmin(true);
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        ApiClientDetails sl = client("MEMBER-1");
        sl.setSlClient(true);
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(sl);

        assertThatThrownBy(() -> apiCredentialsResource.updateApiClient("APP-123", new ApiClientDetails()))
                .isInstanceOf(AccessDeniedException.class);
        verify(apiClientDetailsService, never()).updateApiClient(any(), any());
    }

    @Test
    void updateApiClient_WhenClientIsDeactivated_ShouldThrowBadRequest() {
        mockUser.setAdmin(true);
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        ApiClientDetails deactivated = client("MEMBER-1");
        deactivated.setDeactivatedDate(LocalDateTime.now());
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(deactivated);

        assertThatThrownBy(() -> apiCredentialsResource.updateApiClient("APP-123", new ApiClientDetails()))
                .isInstanceOf(BadRequestAlertException.class);
    }

    @Test
    void updateApiClient_WhenClientMissing_ShouldThrow404() {
        when(apiClientDetailsService.getApiClientDetails("APP-404")).thenReturn(null);

        assertThatThrownBy(() -> apiCredentialsResource.updateApiClient("APP-404", new ApiClientDetails()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getClientSecret_WhenUserBelongsToMember_ShouldReturnSecretWithNoStore() {
        mockUser.setAdmin(false);
        mockUser.setMemberId("MEMBER-MATCH");
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(client("MEMBER-MATCH"));
        when(apiClientDetailsService.getClientSecret("APP-123")).thenReturn("s3cret");

        ResponseEntity<ApiClientSecret> response = apiCredentialsResource.getClientSecret("APP-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getClientSecret()).isEqualTo("s3cret");
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
    }

    @Test
    void getClientSecret_ForSlClient_ShouldStillBeAllowed() {
        mockUser.setAdmin(true);
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        ApiClientDetails sl = client("MEMBER-1");
        sl.setSlClient(true);
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(sl);
        when(apiClientDetailsService.getClientSecret("APP-123")).thenReturn("s3cret");

        assertThat(apiCredentialsResource.getClientSecret("APP-123").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getClientSecret_WhenUserDoesNotBelongToMember_ShouldThrowAccessDenied() {
        mockUser.setAdmin(false);
        mockUser.setMemberId("MEMBER-OTHER");
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(client("MEMBER-TARGET"));

        assertThatThrownBy(() -> apiCredentialsResource.getClientSecret("APP-123"))
                .isInstanceOf(AccessDeniedException.class);
        verify(apiClientDetailsService, never()).getClientSecret(any());
    }

    @Test
    void getClientSecret_WhenClientMissing_ShouldReturn404() {
        when(apiClientDetailsService.getApiClientDetails("APP-404")).thenReturn(null);

        assertThat(apiCredentialsResource.getClientSecret("APP-404").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void resetClientSecret_WhenUserBelongsToMember_ShouldReturnNewSecretWithNoStore() {
        mockUser.setAdmin(false);
        mockUser.setMemberId("MEMBER-MATCH");
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        ApiClientDetails existing = client("MEMBER-MATCH");
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(existing);
        when(apiClientDetailsService.resetClientSecret(existing)).thenReturn("new-secret");

        ResponseEntity<ApiClientSecret> response = apiCredentialsResource.resetClientSecret("APP-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getClientSecret()).isEqualTo("new-secret");
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
    }

    @Test
    void resetClientSecret_WhenClientIsSl_ShouldThrowAccessDenied() {
        mockUser.setAdmin(true);
        when(userService.getLoggedInUser()).thenReturn(mockUser);
        ApiClientDetails sl = client("MEMBER-1");
        sl.setSlClient(true);
        when(apiClientDetailsService.getApiClientDetails("APP-123")).thenReturn(sl);

        assertThatThrownBy(() -> apiCredentialsResource.resetClientSecret("APP-123"))
                .isInstanceOf(AccessDeniedException.class);
        verify(apiClientDetailsService, never()).resetClientSecret(any());
    }

    private ApiClientDetails client(String memberId) {
        ApiClientDetails details = new ApiClientDetails();
        details.setClientDetailsId("APP-123");
        details.setMemberId(memberId);
        return details;
    }
}