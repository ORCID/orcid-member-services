package org.orcid.mp.member.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.orcid.mp.member.apicreds.*;
import org.orcid.mp.member.client.ApiCredentialsServiceClient;
import org.orcid.mp.member.domain.Member;
import org.orcid.mp.member.domain.User;
import org.orcid.mp.member.error.ApiClientValidationException;
import org.orcid.mp.member.salesforce.MemberContact;
import org.orcid.mp.member.salesforce.MemberContacts;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private SalesforceService salesforceService;

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
    void getApiClientsForMember_ReturnsUpstreamSlClientFlagWithoutFetchingDetails() {
        ApiClientSummary slSummary = new ApiClientSummary();
        slSummary.setClientDetailsId("APP-SL");
        slSummary.setSlClient(true);
        ApiClientSummary normalSummary = new ApiClientSummary();
        normalSummary.setClientDetailsId("APP-1");
        ApiClientSummaryPage page = new ApiClientSummaryPage();
        page.setContent(List.of(slSummary, normalSummary));
        when(apiCredentialsServiceClient.getApiClientsForMember("MEMBER-1")).thenReturn(page);

        ApiClientSummaryPage result = apiClientDetailsService.getApiClientsForMember("MEMBER-1");

        assertSame(page, result);
        assertTrue(slSummary.isSlClient());
        assertFalse(normalSummary.isSlClient());
        verify(apiCredentialsServiceClient, never()).getApiClientDetails(anyString());
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
    void updateApiClient_ShouldKeepMemberNameAndExistingRedirectUriTypes() {
        mockClientMember();

        ApiClientDetails existing = existingClient();
        ApiClientDetails request = new ApiClientDetails();
        request.setMemberId("");
        request.setClientName("Hacked name");
        request.setWebsite("https://new.example.org");
        request.setDescription("new description");
        request.setRedirectUris(new LinkedHashSet<>(List.of(
                new ApiClientRedirectUri("https://a.example.org", "default", null),
                new ApiClientRedirectUri("https://b.example.org", "default", null))));

        when(apiCredentialsServiceClient.update(eq("APP-123"), any())).thenAnswer(i -> i.getArgument(1));

        apiClientDetailsService.updateApiClient(existing, request);

        ArgumentCaptor<ApiClientDetails> captor = ArgumentCaptor.forClass(ApiClientDetails.class);
        verify(apiCredentialsServiceClient).update(eq("APP-123"), captor.capture());
        ApiClientDetails sent = captor.getValue();
        assertThat(sent.getMemberId()).isEqualTo("M1");
        assertThat(sent.getClientName()).isEqualTo("Original name");
        assertThat(sent.getWebsite()).isEqualTo("https://new.example.org");
        assertThat(sent.getDescription()).isEqualTo("new description");
        assertThat(sent.getRedirectUris()).extracting(ApiClientRedirectUri::getRedirectUri, ApiClientRedirectUri::getRedirectUriType)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("https://a.example.org", "grant-read-wizard"),
                        org.assertj.core.groups.Tuple.tuple("https://b.example.org", "default"));
    }

    @Test
    void updateApiClient_ShouldDropBlankAndDuplicateUris() {
        mockClientMember();

        ApiClientDetails request = new ApiClientDetails();
        request.setRedirectUris(new LinkedHashSet<>(List.of(
                new ApiClientRedirectUri("https://a.example.org", "default", null),
                new ApiClientRedirectUri("  https://a.example.org ", "default", null),
                new ApiClientRedirectUri("   ", "default", null))));

        when(apiCredentialsServiceClient.update(eq("APP-123"), any())).thenAnswer(i -> i.getArgument(1));

        apiClientDetailsService.updateApiClient(existingClient(), request);

        ArgumentCaptor<ApiClientDetails> captor = ArgumentCaptor.forClass(ApiClientDetails.class);
        verify(apiCredentialsServiceClient).update(eq("APP-123"), captor.capture());
        assertThat(captor.getValue().getRedirectUris()).extracting(ApiClientRedirectUri::getRedirectUri)
                .containsExactly("https://a.example.org");
    }

    @Test
    void updateApiClient_ShouldEmailOnlyWhatChanged() {
        mockClientMember();
        when(apiCredentialsServiceClient.update(eq("APP-123"), any())).thenAnswer(i -> i.getArgument(1));

        ApiClientDetails existing = existingClient();
        existing.setWebsite("https://old.example.org");
        existing.setDescription("Same description");
        ApiClientDetails request = new ApiClientDetails();
        request.setWebsite("https://new.example.org");
        request.setDescription("Same description");
        request.setRedirectUris(new LinkedHashSet<>(List.of(
                new ApiClientRedirectUri("https://a.example.org", "default", null),
                new ApiClientRedirectUri("https://b.example.org", "default", null))));

        apiClientDetailsService.updateApiClient(existing, request);

        ArgumentCaptor<ApiClientUpdateNotification> captor = ArgumentCaptor.forClass(ApiClientUpdateNotification.class);
        verify(mailService).sendClientUpdatedEmail(captor.capture());
        ApiClientUpdateNotification notification = captor.getValue();
        assertThat(notification.getClientId()).isEqualTo("APP-123");
        assertThat(notification.getClientName()).isEqualTo("Original name");
        assertThat(notification.getHomepageUrl()).isEqualTo("https://new.example.org");
        assertThat(notification.getDescription()).isNull();
        assertThat(notification.getAddedRedirectUris()).containsExactly("https://b.example.org");
        assertThat(notification.getRemovedRedirectUris()).containsExactly("https://old.example.org");
        assertThat(notification.isSecretReset()).isFalse();
    }

    @Test
    void updateApiClient_ShouldSendToSalesforceMainAndTechContactsOwnersAndApiCredsManagers() {
        mockClientMember();
        MemberContacts contacts = new MemberContacts();
        contacts.setRecords(List.of(
                contact("main@member.org", SalesforceService.MAIN_CONTACT_ROLE),
                contact("tech@member.org", SalesforceService.TECHNICAL_CONTACT_ROLE),
                contact("product@member.org", "Product Contact")));
        when(salesforceService.getMemberContacts("SF-1")).thenReturn(contacts);
        when(userService.getUsersByMemberId("M1")).thenReturn(List.of(
                user("owner@member.org", true, false),
                user("MAIN@member.org", true, false),
                user("creds@member.org", false, true),
                user("regular@member.org", false, false)));
        when(apiCredentialsServiceClient.update(eq("APP-123"), any())).thenAnswer(i -> i.getArgument(1));

        ApiClientDetails request = new ApiClientDetails();
        request.setWebsite("https://new.example.org");
        apiClientDetailsService.updateApiClient(existingClient(), request);

        ArgumentCaptor<ApiClientUpdateNotification> captor = ArgumentCaptor.forClass(ApiClientUpdateNotification.class);
        verify(mailService).sendClientUpdatedEmail(captor.capture());
        assertThat(captor.getValue().getRecipients())
                .containsExactly("main@member.org", "tech@member.org", "owner@member.org", "creds@member.org");
    }

    @Test
    void updateApiClient_WhenSalesforceFails_ShouldStillEmailPortalUsers() {
        mockClientMember();
        when(salesforceService.getMemberContacts("SF-1")).thenThrow(new RuntimeException("salesforce down"));
        when(userService.getUsersByMemberId("M1")).thenReturn(List.of(user("owner@member.org", true, false)));
        when(apiCredentialsServiceClient.update(eq("APP-123"), any())).thenAnswer(i -> i.getArgument(1));

        ApiClientDetails request = new ApiClientDetails();
        request.setWebsite("https://new.example.org");
        apiClientDetailsService.updateApiClient(existingClient(), request);

        ArgumentCaptor<ApiClientUpdateNotification> captor = ArgumentCaptor.forClass(ApiClientUpdateNotification.class);
        verify(mailService).sendClientUpdatedEmail(captor.capture());
        assertThat(captor.getValue().getRecipients()).containsExactly("owner@member.org");
    }

    @Test
    void updateApiClient_WhenNoRecipientsFound_ShouldEmailTheRequester() {
        mockClientMember();
        User requester = user("requester@member.org", false, false);
        when(userService.getLoggedInUser()).thenReturn(requester);
        when(apiCredentialsServiceClient.update(eq("APP-123"), any())).thenAnswer(i -> i.getArgument(1));

        ApiClientDetails request = new ApiClientDetails();
        request.setWebsite("https://new.example.org");
        apiClientDetailsService.updateApiClient(existingClient(), request);

        ArgumentCaptor<ApiClientUpdateNotification> captor = ArgumentCaptor.forClass(ApiClientUpdateNotification.class);
        verify(mailService).sendClientUpdatedEmail(captor.capture());
        assertThat(captor.getValue().getRecipients()).containsExactly("requester@member.org");
    }

    @Test
    void updateApiClient_WhenNothingChanged_ShouldNotSendEmail() {
        when(apiCredentialsServiceClient.update(eq("APP-123"), any())).thenAnswer(i -> i.getArgument(1));
        ApiClientDetails existing = existingClient();
        existing.setWebsite("https://same.example.org");
        ApiClientDetails request = new ApiClientDetails();
        request.setWebsite(" https://same.example.org ");
        request.setRedirectUris(new LinkedHashSet<>(List.of(
                new ApiClientRedirectUri("https://a.example.org", "default", null),
                new ApiClientRedirectUri("https://old.example.org", "default", null))));

        apiClientDetailsService.updateApiClient(existing, request);

        verify(mailService, never()).sendClientUpdatedEmail(any());
    }

    @Test
    void updateApiClient_WhenUpstreamThrows_ShouldNotSendEmail() {
        when(apiCredentialsServiceClient.update(eq("APP-123"), any()))
                .thenThrow(new ApiClientValidationException(List.of("Invalid website")));

        assertThatThrownBy(() -> apiClientDetailsService.updateApiClient(existingClient(), new ApiClientDetails()))
                .isInstanceOf(ApiClientValidationException.class);

        verify(mailService, never()).sendClientUpdatedEmail(any());
    }

    @Test
    void resetClientSecret_ShouldResetThenFetchSecretAndSendEmail() {
        mockClientMember();
        when(userService.getUsersByMemberId("M1")).thenReturn(List.of(user("owner@member.org", true, false)));
        when(apiCredentialsServiceClient.resetClientSecret("APP-123")).thenReturn(true);
        when(apiCredentialsServiceClient.getClientSecret("APP-123")).thenReturn("new-secret");

        String secret = apiClientDetailsService.resetClientSecret(existingClient());

        assertThat(secret).isEqualTo("new-secret");
        ArgumentCaptor<ApiClientUpdateNotification> captor = ArgumentCaptor.forClass(ApiClientUpdateNotification.class);
        verify(mailService).sendClientUpdatedEmail(captor.capture());
        assertThat(captor.getValue().isSecretReset()).isTrue();
        assertThat(captor.getValue().getHomepageUrl()).isNull();
        assertThat(captor.getValue().getRecipients()).containsExactly("owner@member.org");
    }

    @Test
    void resetClientSecret_WhenUpstreamReturnsFalse_ShouldThrowAndNotSendEmail() {
        when(apiCredentialsServiceClient.resetClientSecret("APP-123")).thenReturn(false);

        assertThatThrownBy(() -> apiClientDetailsService.resetClientSecret(existingClient()))
                .isInstanceOf(IllegalStateException.class);

        verify(apiCredentialsServiceClient, never()).getClientSecret(anyString());
        verify(mailService, never()).sendClientUpdatedEmail(any());
    }

    private void mockClientMember() {
        Member member = new Member();
        member.setId("M1");
        member.setSalesforceId("SF-1");
        org.mockito.Mockito.lenient().when(memberService.getMember(eq("M1"))).thenReturn(Optional.of(member));
        User requester = user("requester@member.org", false, false);
        org.mockito.Mockito.lenient().when(userService.getLoggedInUser()).thenReturn(requester);
    }

    private MemberContact contact(String email, String role) {
        MemberContact contact = new MemberContact();
        contact.setEmail(email);
        contact.setRole(role);
        return contact;
    }

    private User user(String email, boolean mainContact, boolean manageApiCreds) {
        User user = new User();
        user.setEmail(email);
        user.setMainContact(mainContact);
        user.setManageApiCredsEnabled(manageApiCreds);
        return user;
    }

    private ApiClientDetails existingClient() {
        ApiClientDetails existing = new ApiClientDetails();
        existing.setClientDetailsId("APP-123");
        existing.setMemberId("M1");
        existing.setClientName("Original name");
        existing.setRedirectUris(new LinkedHashSet<>(List.of(
                new ApiClientRedirectUri("https://a.example.org", "grant-read-wizard", null),
                new ApiClientRedirectUri("https://old.example.org", "default", null))));
        return existing;
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

    @Test
    void applyForAMCredentials_WhenMemberHasNoName_ShouldKeepNameFromRequest() {
        User user = new User();
        user.setMemberId("1");
        user.setFirstName("Sarah");
        user.setLastName("Smith");
        user.setEmail("sarah.smith@example.com");

        when(userService.getLoggedInUser()).thenReturn(user);
        when(memberService.getMember(eq("1"))).thenReturn(Optional.of(new Member()));

        AMCredentialsApplication request = new AMCredentialsApplication();
        request.setOrgName("Typed Org");
        request.setConsortiumLeadName("Typed Lead");
        request.setOrgHomePage("https://typed.example.org");

        apiClientDetailsService.applyForAMCredentials(request);

        assertThat(request.getOrgName()).isEqualTo("Typed Org");
        assertThat(request.getConsortiumLeadName()).isEqualTo("Typed Lead");
        assertThat(request.getOrgHomePage()).isEqualTo("https://typed.example.org");
        assertThat(request.getRequestedByEmail()).isEqualTo("sarah.smith@example.com");
        verify(mailService).applyForAMCreds(request);
    }

    @Test
    void applyForAMCredentials_ShouldNotLetTheRequestOverrideTheRequester() {
        User user = new User();
        user.setMemberId("1");
        user.setFirstName("Sarah");
        user.setLastName("Smith");
        user.setEmail("sarah.smith@example.com");
        Member member = new Member();
        member.setClientName("Real Org");

        when(userService.getLoggedInUser()).thenReturn(user);
        when(memberService.getMember(eq("1"))).thenReturn(Optional.of(member));

        AMCredentialsApplication request = new AMCredentialsApplication();
        request.setOrgName("Spoofed Org");
        request.setRequestedByEmail("attacker@example.com");

        apiClientDetailsService.applyForAMCredentials(request);

        assertThat(request.getOrgName()).isEqualTo("Real Org");
        assertThat(request.getRequestedByEmail()).isEqualTo("sarah.smith@example.com");
    }
}