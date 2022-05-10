package org.orcid.memberportal.service.assertion.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.xml.bind.JAXBException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.orcid.jaxb.model.v3.release.notification.NotificationType;
import org.orcid.jaxb.model.v3.release.notification.permission.Item;
import org.orcid.jaxb.model.v3.release.notification.permission.ItemType;
import org.orcid.jaxb.model.v3.release.notification.permission.NotificationPermission;
import org.orcid.memberportal.service.assertion.client.OrcidAPIClient;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.springframework.context.MessageSource;

class NotificationServiceTest {
    
    @Mock
    private AssertionRepository assertionRepository;
    
    @Mock
    private OrcidRecordService orcidRecordService;
    
    @Mock
    private OrcidAPIClient orcidApiClient;
    
    @Mock
    private MessageSource messageSource;
    
    @Captor
    private ArgumentCaptor<Assertion> assertionCaptor;
    
    @Captor
    private ArgumentCaptor<NotificationPermission> notificationPermissionCaptor;
    
    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        
        Mockito.when(assertionRepository.findEmailAndSalesforceIdsWithNotificationRequested()).thenReturn(getListOfApplicableEmailsAndSalesforceIds());
        
        Mockito.when(assertionRepository.findByEmailAndSalesforceIdAndStatus(Mockito.eq("email1"), Mockito.eq("salesforceId1"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()))).thenReturn(getListOfAssertionsForNotification(1, "email1"));
        Mockito.when(assertionRepository.findByEmailAndSalesforceIdAndStatus(Mockito.eq("email2"), Mockito.eq("salesforceId2"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()))).thenReturn(getListOfAssertionsForNotification(3, "email2"));
        Mockito.when(assertionRepository.findByEmailAndSalesforceIdAndStatus(Mockito.eq("email3"), Mockito.eq("salesforceId3"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()))).thenReturn(getListOfAssertionsForNotification(5, "email3"));
        Mockito.when(assertionRepository.findByEmailAndSalesforceIdAndStatus(Mockito.eq("email4"), Mockito.eq("salesforceId4"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()))).thenReturn(getListOfAssertionsForNotification(7, "email4"));
        Mockito.when(assertionRepository.findByEmailAndSalesforceIdAndStatus(Mockito.eq("email5"), Mockito.eq("salesforceId5"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()))).thenReturn(getListOfAssertionsForNotification(9, "email5"));
        
        Mockito.when(orcidRecordService.generateLinkForEmailAndSalesforceId(Mockito.eq("email1"), Mockito.eq("salesforceId1"))).thenReturn("link1");
        Mockito.when(orcidRecordService.generateLinkForEmailAndSalesforceId(Mockito.eq("email2"), Mockito.eq("salesforceId2"))).thenReturn("link2");
        Mockito.when(orcidRecordService.generateLinkForEmailAndSalesforceId(Mockito.eq("email3"), Mockito.eq("salesforceId3"))).thenReturn("link3");
        Mockito.when(orcidRecordService.generateLinkForEmailAndSalesforceId(Mockito.eq("email4"), Mockito.eq("salesforceId4"))).thenReturn("link4");
        Mockito.when(orcidRecordService.generateLinkForEmailAndSalesforceId(Mockito.eq("email5"), Mockito.eq("salesforceId5"))).thenReturn("link5");
        
        Mockito.when(messageSource.getMessage(Mockito.eq("assertion.notifications.intro"), Mockito.isNull(), Mockito.any(Locale.class))).thenReturn("intro");
        Mockito.when(messageSource.getMessage(Mockito.eq("assertion.notifications.subject"), Mockito.isNotNull(), Mockito.any(Locale.class))).thenReturn("subject");
        
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("email1"))).thenReturn("orcid1");
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("email2"))).thenReturn("orcid2");
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("email3"))).thenReturn("orcid3");
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("email4"))).thenReturn("orcid4");
        Mockito.when(orcidApiClient.getOrcidIdForEmail(Mockito.eq("email5"))).thenReturn(null);
    }

    @Test
    void testSendPermissionLinkNotifications() throws IOException, JAXBException {
        notificationService.sendPermissionLinkNotifications();
        
        Mockito.verify(assertionRepository).findEmailAndSalesforceIdsWithNotificationRequested();
        Mockito.verify(assertionRepository).findByEmailAndSalesforceIdAndStatus(Mockito.eq("email1"), Mockito.eq("salesforceId1"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()));
        Mockito.verify(assertionRepository).findByEmailAndSalesforceIdAndStatus(Mockito.eq("email2"), Mockito.eq("salesforceId2"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()));
        Mockito.verify(assertionRepository).findByEmailAndSalesforceIdAndStatus(Mockito.eq("email3"), Mockito.eq("salesforceId3"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()));
        Mockito.verify(assertionRepository).findByEmailAndSalesforceIdAndStatus(Mockito.eq("email4"), Mockito.eq("salesforceId4"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()));
        Mockito.verify(assertionRepository).findByEmailAndSalesforceIdAndStatus(Mockito.eq("email5"), Mockito.eq("salesforceId5"), Mockito.eq(AssertionStatus.NOTIFICATION_REQUESTED.name()));
        
        Mockito.verify(orcidApiClient).getOrcidIdForEmail(Mockito.eq("email1"));
        Mockito.verify(orcidApiClient).getOrcidIdForEmail(Mockito.eq("email2"));
        Mockito.verify(orcidApiClient).getOrcidIdForEmail(Mockito.eq("email3"));
        Mockito.verify(orcidApiClient).getOrcidIdForEmail(Mockito.eq("email4"));
        Mockito.verify(orcidApiClient).getOrcidIdForEmail(Mockito.eq("email5"));
        
        Mockito.verify(messageSource, Mockito.times(4)).getMessage(Mockito.eq("assertion.notifications.intro"), Mockito.isNull(), Mockito.any(Locale.class));
        Mockito.verify(messageSource, Mockito.times(4)).getMessage(Mockito.eq("assertion.notifications.subject"), Mockito.isNotNull(), Mockito.any(Locale.class));
        
        Mockito.verify(orcidRecordService).generateLinkForEmailAndSalesforceId(Mockito.eq("email1"), Mockito.eq("salesforceId1"));
        Mockito.verify(orcidRecordService).generateLinkForEmailAndSalesforceId(Mockito.eq("email2"), Mockito.eq("salesforceId2"));
        Mockito.verify(orcidRecordService).generateLinkForEmailAndSalesforceId(Mockito.eq("email3"), Mockito.eq("salesforceId3"));
        Mockito.verify(orcidRecordService).generateLinkForEmailAndSalesforceId(Mockito.eq("email4"), Mockito.eq("salesforceId4"));
        Mockito.verify(orcidRecordService, Mockito.never()).generateLinkForEmailAndSalesforceId(Mockito.eq("email5"), Mockito.eq("salesforceId5"));
        
        Mockito.verify(assertionRepository, Mockito.times(25)).save(assertionCaptor.capture()); // 25 total assertions updated (1 + 3 + 5 + 7 + 9)
        List<Assertion> assertionsUpdated = assertionCaptor.getAllValues();
        assertionsUpdated.forEach(a -> {
            if (a.getEmail().equals("email5")) {
                // no orcid id was available for email5
                assertThat(a.getStatus()).isEqualTo(AssertionStatus.PENDING.name());
            } else {
                assertThat(a.getStatus()).isEqualTo(AssertionStatus.NOTIFICATION_SENT.name());
            }
        });
        
        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid1"));
        NotificationPermission notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "1", 1);
        
        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid2"));
        notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "2", 3);
        
        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid3"));
        notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "3", 5);
        
        Mockito.verify(orcidApiClient).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid4"));
        notificationPermission = notificationPermissionCaptor.getValue();
        checkNotificationPermissionObject(notificationPermission, "4", 7);
        
        Mockito.verify(orcidApiClient, Mockito.never()).postNotification(notificationPermissionCaptor.capture(), Mockito.eq("orcid5"));
    }
    
    private void checkNotificationPermissionObject(NotificationPermission notificationPermission, String variant, int expectedNumberOfItems) {
        assertThat(notificationPermission.getNotificationSubject()).isEqualTo("subject");
        assertThat(notificationPermission.getNotificationIntro()).isEqualTo("intro");
        assertThat(notificationPermission.getNotificationType()).isEqualTo(NotificationType.PERMISSION);
        assertThat(notificationPermission.getAuthorizationUrl().getUri()).isEqualTo("link" + variant);
        assertThat(notificationPermission.getItems().getItems().size()).isEqualTo(expectedNumberOfItems);
        for (int i = 0; i < expectedNumberOfItems; i++) {
            Item item = notificationPermission.getItems().getItems().get(i);
            assertThat(item.getItemName()).isEqualTo("org name " + i + " : " + "role " + i);
            assertThat(item.getItemType()).isEqualTo(ItemType.EDUCATION);
        }
    }

    private List<Assertion> getListOfApplicableEmailsAndSalesforceIds() {
        return Arrays.asList(getAssertion(1), getAssertion(2), getAssertion(3), getAssertion(4), getAssertion(5));
    }
    
    private Assertion getAssertion(int i) {
        Assertion a = new Assertion();
        a.setSalesforceId("salesforceId" + i);
        a.setEmail("email" + i);
        a.setStatus(AssertionStatus.NOTIFICATION_REQUESTED.name());
        return a;
    }

    private List<Assertion> getListOfAssertionsForNotification(int size, String email) {
        List<Assertion> assertions = new ArrayList<>();
        for (int x = 0; x < size; x++) {
            Assertion assertion = getAssertion(x);
            assertion.setEmail(email);
            assertion.setRoleTitle("role " + x);
            assertion.setOrgName("org name " + x);
            assertion.setAffiliationSection(AffiliationSection.EDUCATION);
            assertions.add(assertion);
        }
        return assertions;
    }

}
