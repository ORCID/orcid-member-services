package org.orcid.mp.assertion.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.orcid.jaxb.model.v3.release.notification.NotificationType;
import org.orcid.jaxb.model.v3.release.notification.permission.Item;
import org.orcid.jaxb.model.v3.release.notification.permission.ItemType;
import org.orcid.jaxb.model.v3.release.notification.permission.NotificationPermission;
import org.orcid.mp.assertion.AssertionServiceApplication;
import org.orcid.mp.assertion.client.InternalMemberServiceClient;
import org.orcid.mp.assertion.client.InternalUserServiceClient;
import org.orcid.mp.assertion.client.MailgunClient;
import org.orcid.mp.assertion.client.OrcidApiClient;
import org.orcid.mp.assertion.domain.*;
import org.orcid.mp.assertion.repository.AssertionRepository;
import org.orcid.mp.assertion.repository.SendNotificationsRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AssertionServiceApplication.class)
public class NotificationServiceIT {

    @Autowired
    private AssertionRepository assertionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MailService mailService;

    @Autowired
    private SendNotificationsRequestRepository sendNotificationsRequestRepository;

    @Mock
    private InternalMemberServiceClient internalMemberServiceClient;

    @Captor
    private ArgumentCaptor<NotificationPermission> notificationPermissionCaptor;

    @Mock
    private OrcidApiClient orcidApiClient;

    @Mock
    private MailgunClient mailgunClient;

    @Mock
    private InternalUserServiceClient internalUserServiceClient;

    private List<Assertion> persistedAssertions;

    @BeforeEach
    public void setup() throws IOException {
        ReflectionTestUtils.setField(notificationService, "orcidApiClient", orcidApiClient);
        ReflectionTestUtils.setField(notificationService, "internalMemberServiceClient", internalMemberServiceClient);
        ReflectionTestUtils.setField(notificationService, "internalUserServiceClient", internalUserServiceClient);
        ReflectionTestUtils.setField(mailService, "mailgunClient", mailgunClient);

        persistedAssertions = new ArrayList<>();
        persistedAssertions.add(getNotificationRequestedAssertion("1", "0", "salesforceId1"));
        persistedAssertions.add(getNotificationRequestedAssertion("2", "0", "salesforceId1"));
        persistedAssertions.add(getNotificationRequestedAssertion("2", "1", "salesforceId1"));
        persistedAssertions.add(getNotificationRequestedAssertion("3", "0", "salesforceId1"));
        persistedAssertions.add(getNotificationRequestedAssertion("4", "0", "salesforceId1"));
        persistedAssertions.add(getNotificationRequestedAssertion("5", "0", "salesforceId1"));
        persistedAssertions.add(getNotificationRequestedAssertion("5", "1", "salesforceId1"));
        persistedAssertions.add(getNotificationRequestedAssertion("5", "2", "salesforceId1"));
        persistedAssertions.add(getNotificationRequestedAssertion("6", "0", "salesforceId2"));
        persistedAssertions.add(getNotificationRequestedAssertion("7", "0", "salesforceId2"));
        persistedAssertions.add(getNotificationRequestedAssertion("8", "0", "salesforceId2"));
        persistedAssertions.add(getNotificationRequestedAssertion("9", "0", "salesforceId3"));
        persistedAssertions.add(getNotificationRequestedAssertion("9", "1", "salesforceId3"));
        persistedAssertions.add(getNotificationRequestedAssertion("10", "0", "salesforceId3"));
        persistedAssertions.forEach(assertionRepository::save);

        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("1@orcid.org"))).thenReturn("orcid1");
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("2@orcid.org"))).thenReturn("orcid2");
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("3@orcid.org"))).thenReturn(null);
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("4@orcid.org"))).thenReturn("orcid4");
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("5@orcid.org"))).thenReturn("orcid5");
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("6@orcid.org"))).thenReturn(null);
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("7@orcid.org"))).thenReturn(null);
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("8@orcid.org"))).thenReturn(null);
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("9@orcid.org"))).thenReturn("orcid9");
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("10@orcid.org"))).thenReturn("orcid10");

        Mockito.when(internalMemberServiceClient.getMember(Mockito.eq("salesforceId1"))).thenReturn(getMember("member 1"));
        Mockito.when(internalMemberServiceClient.getMember(Mockito.eq("salesforceId2"))).thenReturn(getMember("member 2"));
        Mockito.when(internalMemberServiceClient.getMember(Mockito.eq("salesforceId3"))).thenReturn(getMember("member 3"));
        Mockito.when(internalMemberServiceClient.getMember(Mockito.eq("salesforceId4"))).thenReturn(getMember("member 4"));
        Mockito.when(internalMemberServiceClient.getMember(Mockito.eq("salesforceId5"))).thenReturn(getMember("member 5"));

        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email1", "salesforceId1"));
        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email2", "salesforceId2"));
        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email3", "salesforceId3"));
        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email4", "salesforceId4"));
        sendNotificationsRequestRepository.save(getSendNotificationsRequest("email5", "salesforceId5"));

        Mockito.when(internalUserServiceClient.getUser(Mockito.anyString())).thenReturn(getUser());
    }

    @AfterEach
    public void tearDown() {
        persistedAssertions.forEach(assertionRepository::delete);
        sendNotificationsRequestRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "any@orcid.org", authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = "password")
    public void testSendPermissionLinkNotifications() throws Exception {
        notificationService.sendPermissionLinkNotifications();

        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid1"));
        NotificationPermission notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "1", 1);

        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid2"));
        notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "2", 2);

        Mockito.verify(orcidApiClient, Mockito.never()).postNotification(Mockito.any(NotificationPermission.class), Mockito.eq("orcid3"));

        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid4"));
        notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "4", 1);

        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid5"));
        notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "5", 3);

        Mockito.verify(orcidApiClient, Mockito.never()).postNotification(Mockito.any(NotificationPermission.class), Mockito.eq("orcid6"));
        Mockito.verify(orcidApiClient, Mockito.never()).postNotification(Mockito.any(NotificationPermission.class), Mockito.eq("orcid7"));
        Mockito.verify(orcidApiClient, Mockito.never()).postNotification(Mockito.any(NotificationPermission.class), Mockito.eq("orcid8"));

        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid9"));
        notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "9", 2);

        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid10"));
        notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "10", 1);
    }

    @Test
    @WithMockUser(username = "any@orcid.org", authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = "password")
    public void testRequestInProgress() throws Exception {
        boolean requestInProgress = notificationService.requestInProgress("salesforceId1");
        assertThat(requestInProgress).isTrue();

        requestInProgress = notificationService.requestInProgress("salesforceId6");
        assertThat(requestInProgress).isFalse();
    }

    @Test
    @WithMockUser(username = "any@orcid.org", authorities = {"ROLE_ADMIN", "ROLE_USER"}, password = "password")
    public void testCreateSendNotificationsRequest() throws Exception {
        notificationService.createSendNotificationsRequest("email6", "salesforceId6");
        boolean requestInProgress = notificationService.requestInProgress("salesforceId6");
        assertThat(requestInProgress).isTrue();
    }

    private SendNotificationsRequest getSendNotificationsRequest(String email, String salesforceId) {
        SendNotificationsRequest request = new SendNotificationsRequest();
        request.setEmail(email);
        request.setSalesforceId(salesforceId);
        request.setDateRequested(Instant.now());
        return request;
    }

    private void checkNotificationPermissionObject(NotificationPermission notificationPermission, String variant, int expectedNumberOfItems) {
        assertThat(notificationPermission.getNotificationSubject()).isEqualTo("affiliation");
        assertThat(notificationPermission.getNotificationIntro()).isEqualTo(
                "An affiliation with your organization as the source signals to other systems that the information comes from an authoritative source and that they have vouched for the fact that you are (or were) indeed affiliated with them. This allows your data to be reused with a high degree of confidence when automatically filling a growing number of manuscript submission or grant application forms.");
        assertThat(notificationPermission.getNotificationType()).isEqualTo(NotificationType.PERMISSION);
        assertThat(notificationPermission.getAuthorizationUrl().getUri()).isNotEmpty();
        assertThat(notificationPermission.getItems().getItems().size()).isEqualTo(expectedNumberOfItems);
        for (int i = 0; i < expectedNumberOfItems; i++) {
            Item item = notificationPermission.getItems().getItems().get(i);
            assertThat(item.getItemName()).isEqualTo(variant + " org " + i + " : " + variant + " role " + i);
            assertThat(item.getItemType()).isEqualTo(ItemType.EMPLOYMENT);
        }
    }

    private Assertion getNotificationRequestedAssertion(String variant, String subvariant, String salesforceId) {
        Assertion assertion = new Assertion();
        assertion.setEmail(variant + "@orcid.org");
        assertion.setSalesforceId(salesforceId);
        assertion.setAffiliationSection(AffiliationSection.EMPLOYMENT);
        assertion.setOrgName(variant + " org " + subvariant);
        assertion.setRoleTitle(variant + " role " + subvariant);
        assertion.setStatus(AssertionStatus.NOTIFICATION_REQUESTED.name());
        assertion.setOrgCity("some city");
        assertion.setOrgCountry("some country");
        assertion.setDisambiguatedOrgId("some org id");
        assertion.setDisambiguationSource("some source");
        return assertion;
    }

    private Member getMember(String name) {
        Member member = new Member();
        member.setClientName(name);
        member.setDefaultLanguage("en");
        return member;
    }

    private User getUser() {
        User user = new User();
        user.setEmail("notifications@orcid.org");
        user.setLangKey("en");
        user.setSalesforceId("salesforceId1");
        return user;
    }
}
