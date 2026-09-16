package org.orcid.mp.member.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.service.ApiClientDetailsService;
import org.orcid.mp.member.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiCredentialsResourceTest {

    @Mock
    private ApiClientDetailsService apiClientDetailsService;

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

        ApiClientSummaryPage mockPage = new ApiClientSummaryPage();
        when(apiClientDetailsService.getApiClientsForMember(memberId)).thenReturn(mockPage);

        // Act & Assert
        assertThatThrownBy(() -> apiCredentialsResource.getClientsForMember(memberId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("User test@example.com can't access client details of MEMBER-TARGET");
    }
}