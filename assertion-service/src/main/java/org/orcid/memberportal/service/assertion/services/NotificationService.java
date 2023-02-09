package org.orcid.memberportal.service.assertion.services;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.orcid.jaxb.model.v3.release.notification.NotificationType;
import org.orcid.jaxb.model.v3.release.notification.permission.AuthorizationUrl;
import org.orcid.jaxb.model.v3.release.notification.permission.Item;
import org.orcid.jaxb.model.v3.release.notification.permission.ItemType;
import org.orcid.jaxb.model.v3.release.notification.permission.Items;
import org.orcid.jaxb.model.v3.release.notification.permission.NotificationPermission;
import org.orcid.memberportal.service.assertion.client.OrcidAPIClient;
import org.orcid.memberportal.service.assertion.config.ApplicationProperties;
import org.orcid.memberportal.service.assertion.domain.Assertion;
import org.orcid.memberportal.service.assertion.domain.SendNotificationsRequest;
import org.orcid.memberportal.service.assertion.domain.enumeration.AssertionStatus;
import org.orcid.memberportal.service.assertion.repository.AssertionRepository;
import org.orcid.memberportal.service.assertion.repository.SendNotificationsRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
    
    private static final int BATCH_SIZE = 100;

    @Autowired
    private AssertionRepository assertionRepository;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private OrcidAPIClient orcidApiClient;

    @Autowired
    private SendNotificationsRequestRepository sendNotificationsRequestRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private ApplicationProperties applicationProperties;

    public boolean requestInProgress(String salesforceId) {
        return findActiveRequestBySalesforceId(salesforceId) != null;
    }

    public void createSendNotificationsRequest(String userEmail, String salesforceId) {
        if (findActiveRequestBySalesforceId(salesforceId) != null) {
            throw new RuntimeException("Send notifications request already active for " + salesforceId);
        }

        SendNotificationsRequest request = new SendNotificationsRequest();
        request.setEmail(userEmail);
        request.setSalesforceId(salesforceId);
        request.setDateRequested(Instant.now());
        sendNotificationsRequestRepository.insert(request);
    }

    public void sendPermissionLinkNotifications() {
        List<SendNotificationsRequest> requests = sendNotificationsRequestRepository.findActiveRequests();
        requests.forEach(r -> {
            processRequest(r);
            markRequestCompleted(r);
        });
    }

    private void markRequestCompleted(SendNotificationsRequest request) {
        LOG.info("Marking SendNotificationsRequest from user {} (salesforce ID {}) as complete", request.getEmail(), request.getSalesforceId());
        request.setDateCompleted(Instant.now());
        mailService.sendNotificationsSummary(userService.getUserById(request.getEmail()), request.getNotificationsSent(), request.getEmailsSent());
        sendNotificationsRequestRepository.save(request);
    }

    private void processRequest(SendNotificationsRequest request) {
        Iterator<String> emailsWithNotificationsRequested = assertionRepository.findDistinctEmailsWithNotificationRequested(request.getSalesforceId());
        String orgName = memberService.getMemberName(request.getSalesforceId());
        emailsWithNotificationsRequested.forEachRemaining(e -> findAssertionsAndAttemptSend(e, orgName, request));
    }
    
    public void resendNotifications() {
        Pageable pageable = PageRequest.of(0, BATCH_SIZE, new Sort(Direction.ASC, "created"));
        Page<Assertion> assertions = assertionRepository.findNotificationResendCandidates(pageable);
        Map<String, String> usersAndSalesforceIds = new HashMap<>();
        
        // build map of applicable email - salesforceIds
        while (assertions != null && !assertions.isEmpty()) {
            assertions.forEach(a -> {
                if (!orcidRecordService.userHasGrantedOrDeniedPermission(a.getEmail(), a.getSalesforceId())) {
                    Instant firstSent = a.getNotificationSent() != null ? a.getNotificationSent() : a.getInvitationSent();
                    Instant lastSent = a.getNotificationLastSent() != null ? a.getNotificationLastSent() : a.getInvitationLastSent();
                    
                    if (lastSent == null) {
                        // legacy data
                        lastSent = firstSent;
                    }
                    
                    for (int days : applicationProperties.getResendNotificationDays()) {
                        Instant now = Instant.now();
                        Instant notificationDue = firstSent.plus(days, ChronoUnit.DAYS);
                        
                        if (now.isAfter(notificationDue) && notificationDue.isAfter(lastSent)) {
                            usersAndSalesforceIds.put(a.getEmail() + ":" + a.getSalesforceId(), null);
                        }
                    }
                }
            });
            pageable = pageable.next();
            assertions = assertionRepository.findNotificationResendCandidates(pageable);
        }
        
        // iterate through data resending notifications
        for (String key : usersAndSalesforceIds.keySet()) {
            String[] keyParts = key.split(":");
            String email = keyParts[0];
            String salesforceId = keyParts[1];
            findAssertionsAndAttemptSend(email, salesforceId);
        }
    }

    private void findAssertionsAndAttemptSend(String email, String salesforceId) {
        String orgName = memberService.getMemberName(salesforceId);
        List<Assertion> allAssertionsForEmailAndMember = assertionRepository.findByEmailAndSalesforceId(email, salesforceId);
        try {
            String orcidId = orcidApiClient.getOrcidIdForEmail(email);
            if (orcidId == null) {
                LOG.info("No ORCID id found for {}. Sending email invitation instead.", email);
                sendEmailInvitation(email, orgName, salesforceId, allAssertionsForEmailAndMember);
            } else {
                LOG.info("ORCID id found for {}. Sending notification.", email);
                sendNotification(email, orgName, salesforceId, allAssertionsForEmailAndMember, orcidId);
            }
        } catch (Exception e) {
            LOG.warn("Error sending notification to {} on behalf of {}", email, salesforceId);
            LOG.warn("Could not send notification", e);
            allAssertionsForEmailAndMember.forEach(a -> {
                a.setStatus(AssertionStatus.NOTIFICATION_FAILED.name());
                assertionRepository.save(a);
            });
        }
    }
    
    private void findAssertionsAndAttemptSend(String email, String orgName, SendNotificationsRequest request) {
        List<Assertion> allAssertionsForEmailAndMember = assertionRepository.findByEmailAndSalesforceIdAndStatus(email, request.getSalesforceId(),
                AssertionStatus.NOTIFICATION_REQUESTED.name());
        try {
            String orcidId = orcidApiClient.getOrcidIdForEmail(email);
            if (orcidId == null) {
                LOG.info("No ORCID id found for {}. Sending email invitation instead.", email);
                sendEmailInvitation(email, orgName, request.getSalesforceId(), allAssertionsForEmailAndMember);
                request.setEmailsSent(request.getEmailsSent() + 1);
            } else {
                LOG.info("ORCID id found for {}. Sending notification.", email);
                sendNotification(email, orgName, request.getSalesforceId(), allAssertionsForEmailAndMember, orcidId);
                request.setNotificationsSent(request.getNotificationsSent() + 1);
            }
        } catch (Exception e) {
            LOG.warn("Error sending notification to {} on behalf of {}", email, request.getSalesforceId());
            LOG.warn("Could not send notification", e);
            allAssertionsForEmailAndMember.forEach(a -> {
                a.setStatus(AssertionStatus.NOTIFICATION_FAILED.name());
                assertionRepository.save(a);
            });
        }
    }

    private void sendNotification(String email, String orgName, String salesforceId, List<Assertion> allAssertionsForEmailAndMember, String orcidId)
            throws JAXBException, IOException {
        NotificationPermission notification = getPermissionLinkNotification(allAssertionsForEmailAndMember, email, salesforceId, orgName);
        orcidApiClient.postNotification(notification, orcidId);
        allAssertionsForEmailAndMember.forEach(a -> {
            a.setStatus(AssertionStatus.NOTIFICATION_SENT.name());
            
            Instant now = Instant.now();
            if (a.getNotificationSent() == null && a.getInvitationSent() == null) {
                // invitation / notification not previously sent
                a.setNotificationSent(now);
            }
            a.setNotificationLastSent(now);
            assertionRepository.save(a);
        });
    }

    private void sendEmailInvitation(String email, String orgName, String salesforceId, List<Assertion> allAssertionsForEmailAndMember) {
        mailService.sendInvitationEmail(email, orgName, orcidRecordService.generateLinkForEmailAndSalesforceId(email, salesforceId));
        allAssertionsForEmailAndMember.forEach(a -> {
            a.setStatus(AssertionStatus.NOTIFICATION_SENT.name());
            
            Instant now = Instant.now();
            if (a.getInvitationSent() == null) {
                // first invitation sent
                a.setInvitationSent(now);
            }
            a.setInvitationLastSent(now);
            assertionRepository.save(a);
        });
    }

    private NotificationPermission getPermissionLinkNotification(List<Assertion> assertions, String email, String salesforceId, String orgName) {
        NotificationPermission notificationPermission = new NotificationPermission();
        notificationPermission.setNotificationIntro(messageSource.getMessage("assertion.notifications.introduction", null, Locale.getDefault()));
        notificationPermission.setNotificationSubject(messageSource.getMessage("assertion.notifications.subject", new Object[] { orgName }, Locale.getDefault()));
        notificationPermission.setNotificationType(NotificationType.PERMISSION);
        notificationPermission.setAuthorizationUrl(new AuthorizationUrl(orcidRecordService.generateLinkForEmailAndSalesforceId(email, salesforceId)));

        List<Item> items = new ArrayList<>();
        assertions.forEach(a -> {
            Item item = new Item();
            item.setItemName(a.getOrgName() + (a.getRoleTitle() != null ? " : " + a.getRoleTitle() : ""));

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

    private SendNotificationsRequest findActiveRequestBySalesforceId(String salesforceId) {
        List<SendNotificationsRequest> requests = sendNotificationsRequestRepository.findActiveRequestBySalesforceId(salesforceId);
        assert requests.size() <= 1;
        return requests.size() == 1 ? requests.get(0) : null;
    }

}
