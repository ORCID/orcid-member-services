package org.orcid.mp.member.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
import org.orcid.mp.member.client.ApiCredentialsServiceClient;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiClientDetailsServiceTest {

    @Mock
    private ApiCredentialsServiceClient apiCredentialsServiceClient;

    @InjectMocks
    private ApiClientDetailsService apiClientDetailsService;

    @Test
    void getApiClientDetails_ShouldReturnClientDetails() {
        // Arrange
        String clientId = "APP-123";
        ApiClientDetails expectedClient = new ApiClientDetails();
        when(apiCredentialsServiceClient.getApiClientDetails(clientId)).thenReturn(expectedClient);

        // Act
        ApiClientDetails actualClient = apiClientDetailsService.getApiClientDetails(clientId);

        // Assert
        assertSame(expectedClient, actualClient, "The returned ApiClientDetails should match the mock exactly");
        verify(apiCredentialsServiceClient).getApiClientDetails(clientId);
    }

    @Test
    void getApiClientDetails_WhenClientReturnsNull_ShouldReturnNull() {
        // Arrange
        String clientId = "APP-MISSING";
        when(apiCredentialsServiceClient.getApiClientDetails(clientId)).thenReturn(null);

        // Act
        ApiClientDetails actualClient = apiClientDetailsService.getApiClientDetails(clientId);

        // Assert
        assertNull(actualClient);
        verify(apiCredentialsServiceClient).getApiClientDetails(clientId);
    }

    @Test
    void getApiClientsForMember_ShouldReturnSummaryPage() {
        // Arrange
        String memberId = "MEMBER-1";
        ApiClientSummaryPage expectedPage = new ApiClientSummaryPage();
        when(apiCredentialsServiceClient.getApiClientsForMember(memberId)).thenReturn(expectedPage);

        // Act
        ApiClientSummaryPage actualPage = apiClientDetailsService.getApiClientsForMember(memberId);

        // Assert
        assertSame(expectedPage, actualPage, "The returned ApiClientSummaryPage should match the mock exactly");
        verify(apiCredentialsServiceClient).getApiClientsForMember(memberId);
    }

    @Test
    void getApiClientsForMember_WhenClientReturnsNull_ShouldReturnNull() {
        // Arrange
        String memberId = "MEMBER-MISSING";
        when(apiCredentialsServiceClient.getApiClientsForMember(memberId)).thenReturn(null);

        // Act
        ApiClientSummaryPage actualPage = apiClientDetailsService.getApiClientsForMember(memberId);

        // Assert
        assertNull(actualPage);
        verify(apiCredentialsServiceClient).getApiClientsForMember(memberId);
    }
}