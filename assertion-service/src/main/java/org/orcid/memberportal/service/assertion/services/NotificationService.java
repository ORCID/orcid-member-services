package org.orcid.memberportal.service.assertion.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.orcid.jaxb.model.v3.release.notification.NotificationType;
import org.orcid.jaxb.model.v3.release.notification.permission.AuthorizationUrl;
import org.orcid.jaxb.model.v3.release.notification.permission.Item;
import org.orcid.jaxb.model.v3.release.notification.permission.ItemType;
import org.orcid.jaxb.model.v3.release.notification.permission.Items;
import org.orcid.jaxb.model.v3.release.notification.permission.NotificationPermission;
import org.orcid.memberportal.service.assertion.client.OrcidAPIClient;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private AssertionRepository assertionRepository;
    
    @Autowired
    private OrcidRecordService orcidRecordService;
    
    @Autowired
    private MessageSource messageSource;

    @Autowired
    private OrcidAPIClient orcidApiClient;

    public void sendPermissionLinkNotifications() {
        List<Assertion> notificationRequestedEmailsAndSalesforceIds = assertionRepository.findEmailAndSalesforceIdsWithNotificationRequested();
        notificationRequestedEmailsAndSalesforceIds.forEach(this::findAssertionsAndAttemptSend);
    }

    private void findAssertionsAndAttemptSend(Assertion assertion) {
        List<Assertion> allAssertionsForEmailAndMember = assertionRepository.findByEmailAndSalesforceIdAndStatus(assertion.getEmail(), assertion.getSalesforceId(),
                AssertionStatus.NOTIFICATION_REQUESTED.name());
        try {
            String email = assertion.getEmail();
            String salesforceId = assertion.getSalesforceId();
            String orgName = assertion.getOrgName();
            String orcidId = orcidApiClient.getOrcidIdForEmail(email);
            if (orcidId == null) {
                allAssertionsForEmailAndMember.forEach(a -> {
                    a.setStatus(AssertionStatus.PENDING.name());
                    assertionRepository.save(a);
                });
            } else {
                NotificationPermission notification = getPermissionLinkNotification(allAssertionsForEmailAndMember, email, salesforceId, orgName);
                orcidApiClient.postNotification(notification, orcidId);
                allAssertionsForEmailAndMember.forEach(a -> {
                    a.setStatus(AssertionStatus.NOTIFICATION_SENT.name());
                    assertionRepository.save(a);
                });
            }
        } catch (Exception e) {
            LOG.warn("Error sending notification to {} on behalf of {}", new Object[] { assertion.getEmail(), assertion.getOrgName() }, e);
            throw new RuntimeException("Error sending notification to " + assertion.getEmail(), e);
        }
    }

    private NotificationPermission getPermissionLinkNotification(List<Assertion> assertions, String email, String salesforceId, String orgName) {
        NotificationPermission notificationPermission = new NotificationPermission();
        notificationPermission.setNotificationIntro(messageSource.getMessage("assertion.notifications.intro", null, Locale.getDefault()));
        notificationPermission.setNotificationSubject(messageSource.getMessage("assertion.notifications.subject", new Object[] { orgName }, Locale.getDefault()));
        notificationPermission.setNotificationType(NotificationType.PERMISSION);
        notificationPermission.setAuthorizationUrl(new AuthorizationUrl(orcidRecordService.generateLinkForEmailAndSalesforceId(email, salesforceId)));
        

        List<Item> items = new ArrayList<>();
        assertions.forEach(a -> {
            Item item = new Item();
            item.setItemName(a.getOrgName() + " : " + a.getRoleTitle());

            switch (a.getAffiliationSection()) {
            case DISTINCTION:
                item.setItemType(ItemType.DISTINCTION);
                break;
            case EDUCATION:
                item.setItemType(ItemType.EDUCATION);
                break;
            case EMPLOYMENT:
                item.setItemType(ItemType.EMPLOYMENT);
                break;
            case INVITED_POSITION:
                item.setItemType(ItemType.INVITED_POSITION);
                break;
            case MEMBERSHIP:
                item.setItemType(ItemType.MEMBERSHIP);
                break;
            case QUALIFICATION:
                item.setItemType(ItemType.QUALIFICATION);
                break;
            case SERVICE:
                item.setItemType(ItemType.SERVICE);
                break;
            default:
                throw new IllegalArgumentException("Invalid item type found");
            }
            items.add(item);
        });

        notificationPermission.setItems(new Items(items));
        return notificationPermission;
    }
    
}
