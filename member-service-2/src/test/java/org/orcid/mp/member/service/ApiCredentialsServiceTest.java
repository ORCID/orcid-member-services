package org.orcid.mp.member.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orcid.mp.member.apicreds.ApiClientDetails;
import org.orcid.mp.member.apicreds.ApiClientSummaryPage;
import org.orcid.mp.member.apicreds.ProductionCredentialsApplication;
import org.orcid.mp.member.client.ApiCredentialsServiceClient;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiCredentialsServiceTest {

    @Mock
    private ApiCredentialsServiceClient apiCredentialsServiceClient;

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private ApiCredentialsService apiClientDetailsService;

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

    @Test
    void applyForApiCredentials_ShouldSetRequesterDetailsAndSendEmail() {
        // Arrange
        User loggedInUser = new User();
        loggedInUser.setFirstName("Jane");
        loggedInUser.setLastName("Doe");
        loggedInUser.setEmail("jane.doe@orcid.org");
        when(userService.getLoggedInUser()).thenReturn(loggedInUser);

        Member member = new Member();
        member.setClientName("Test Member");
        when(memberService.getMember(loggedInUser.getMemberId())).thenReturn(Optional.of(member));

        ProductionCredentialsApplication application = new ProductionCredentialsApplication();

        // Act
        apiClientDetailsService.applyForApiCredentials(application);

        // Assert
        assertThat(application.getRequestedByName()).isEqualTo("Jane Doe");
        assertThat(application.getRequestedByEmail()).isEqualTo("jane.doe@orcid.org");
        assertThat(application.getOrgName()).isEqualTo("Test Member");
        verify(userService).getLoggedInUser();
        verify(mailService).sendApplyForProdCredsEmail(application);
    }
}