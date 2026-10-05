package org.orcid.mp.member.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orcid.mp.member.apicreds.*;
import org.orcid.mp.member.client.ApiCredentialsServiceClient;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

    @Test
    void applyForApiCredentials_ShouldSendToCLToo() {
        // Arrange
        User loggedInUser = new User();
        loggedInUser.setFirstName("Jane");
        loggedInUser.setLastName("Doe");
        loggedInUser.setEmail("jane.doe@orcid.org");
        when(userService.getLoggedInUser()).thenReturn(loggedInUser);

        Member member = new Member();
        member.setClientName("Test Member");
        member.setParentSalesforceId("sf-id");
        when(memberService.getMember(loggedInUser.getMemberId())).thenReturn(Optional.of(member));

        Member cl = new Member();
        cl.setId("1");
        cl.setSalesforceId("sf-id");
        when(memberService.getMember(eq("sf-id"))).thenReturn(Optional.of(cl));

        User clMainContact = new User();
        clMainContact.setMainContact(true);
        clMainContact.setMemberId("1");
        clMainContact.setEmail("clMainContact@orcid.org");
        when(userService.getUsersByMemberId(eq("1"))).thenReturn(List.of(clMainContact));

        ProductionCredentialsApplication application = new ProductionCredentialsApplication();

        // Act
        apiClientDetailsService.applyForApiCredentials(application);

        // Assert
        assertThat(application.getRequestedByName()).isEqualTo("Jane Doe");
        assertThat(application.getRequestedByEmail()).isEqualTo("jane.doe@orcid.org");
        assertThat(application.getOrgName()).isEqualTo("Test Member");
        assertThat(application.getConsortiumLeadEmail()).isEqualTo("clMainContact@orcid.org");
        verify(userService).getLoggedInUser();
        verify(mailService).sendApplyForProdCredsEmail(application);
    }

    @Test
    void requestSLClientChange_MutatesRequestAndCallsMailService() {
        // Arrange
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setMemberId("12345");

        Member member = new Member();
        member.setClientName("Test Org");

        when(userService.getLoggedInUser()).thenReturn(user);
        when(memberService.getMember(eq("12345"))).thenReturn(Optional.of(member));

        SlClientChangeRequest request = new SlClientChangeRequest();

        // Act
        apiClientDetailsService.requestSLClientChange(request);

        // Assert
        assertThat("John Doe").isEqualTo(request.getRequestedByName());
        assertThat("john.doe@example.com").isEqualTo(request.getRequestedByEmail());
        assertThat("Test Org").isEqualTo(request.getOrgName());

        verify(mailService).requestSLClientChange(request);
    }

    @Test
    void requestSLClientChange_ThrowsExceptionWhenMemberNotFound() {
        // Arrange
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@example.com");
        user.setMemberId("12345");

        when(userService.getLoggedInUser()).thenReturn(user);
        when(memberService.getMember(eq("12345"))).thenReturn(Optional.empty());

        SlClientChangeRequest request = new SlClientChangeRequest();

        // Act & Assert
        assertThrows(NoSuchElementException.class, () ->
                apiClientDetailsService.requestSLClientChange(request)
        );

        verifyNoInteractions(mailService);
    }

    @Test
    void applyForAMCredentials_MutatesRequestAndCallsMailService() {
        // Arrange
        User user = new User();
        user.setMemberId("1");
        user.setFirstName("Sarah");
        user.setLastName("Smith");
        user.setEmail("sarah.smith@example.com");
        user.setMemberId("1");

        Member member = new Member();
        member.setClientName("Test AM Org");

        when(userService.getLoggedInUser()).thenReturn(user);
        when(memberService.getMember(eq("1"))).thenReturn(Optional.of(member));

        AMCredentialsApplication request = new AMCredentialsApplication();

        // Act
        apiClientDetailsService.applyForAMCredentials(request);

        // Assert
        assertThat(request.getRequestedByName()).isEqualTo("Sarah Smith");
        assertThat(request.getRequestedByEmail()).isEqualTo("sarah.smith@example.com");
        assertThat(request.getOrgName()).isEqualTo("Test AM Org");

        verify(mailService).applyForAMCreds(request);
    }
}