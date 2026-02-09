package org.orcid.mp.assertion.service;

import org.orcid.jaxb.model.v3.release.notification.NotificationType;
import org.orcid.jaxb.model.v3.release.notification.permission.*;
import org.orcid.mp.assertion.client.InternalMemberServiceClient;
import org.orcid.mp.assertion.client.InternalUserServiceClient;
import org.orcid.mp.assertion.client.OrcidApiClient;
import org.orcid.mp.assertion.domain.Assertion;
import org.orcid.mp.assertion.domain.AssertionStatus;
import org.orcid.mp.assertion.domain.Member;
import org.orcid.mp.assertion.domain.SendNotificationsRequest;
import org.orcid.mp.assertion.repository.AssertionRepository;
import org.orcid.mp.assertion.repository.SendNotificationsRequestRepository;
import org.orcid.mp.assertion.util.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    private static final int BATCH_SIZE = 100;

    @Autowired
    private MailService mailService;

    @Autowired
    private SendNotificationsRequestRepository sendNotificationsRequestRepository;

    @Autowired
    private InternalUserServiceClient internalUserServiceClient;

    @Autowired
    private InternalMemberServiceClient internalMemberServiceClient;

    @Autowired
    private AssertionRepository assertionRepository;

    @Autowired
    private OrcidRecordService orcidRecordService;

    @Autowired
    private OrcidApiClient orcidApiClient;

    @Autowired
    private MessageSource messageSource;

    @Value("${application.notifications.resendNotificationDays}")
    private int[] resendNotificationDays;

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

    public boolean requestInProgress(String salesforceId) {
        return findActiveRequestBySalesforceId(salesforceId) != null;
    }

    public void sendPermissionLinkNotifications() {
        List<SendNotificationsRequest> requests = sendNotificationsRequestRepository.findActiveRequests();
        requests.forEach(r -> {
            processRequest(r);
            markRequestCompleted(r);
        });
    }

    public void resendNotifications() {
        Pageable pageable = PageRequest.of(0, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "created"));
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

                    for (int days : resendNotificationDays) {
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

        resendNotifications(usersAndSalesforceIds);
    }

    private SendNotificationsRequest findActiveRequestBySalesforceId(String salesforceId) {
        List<SendNotificationsRequest> requests = sendNotificationsRequestRepository.findActiveRequestBySalesforceId(salesforceId);
        assert requests.size() <= 1;
        return requests.size() == 1 ? requests.get(0) : null;
    }

    private void resendNotifications(Map<String, String> usersAndSalesforceIds) {
        for (String key : usersAndSalesforceIds.keySet()) {
            String[] keyParts = key.split(":");
            String email = keyParts[0];
            String salesforceId = keyParts[1];

            LOG.info("Attempting to resend notification / invitation to {} for salesforce id {}", email, salesforceId);
            findAssertionsAndAttemptSend(email, salesforceId);
        }
    }

    private void findAssertionsAndAttemptSend(String email, String salesforceId) {
        String orgName = internalMemberServiceClient.getMember(salesforceId).getClientName();
        String language = internalMemberServiceClient.getMember(salesforceId).getDefaultLanguage();

        List<Assertion> allAssertionsForEmailAndMember = assertionRepository.findByEmailAndSalesforceId(email, salesforceId);
        try {
            String orcidId = orcidApiClient.getOrcidIdForEmail(email);
            if (orcidId == null) {
                LOG.info("No ORCID id found for {}. Sending email invitation instead.", email);
                sendEmailInvitation(email, orgName, salesforceId, allAssertionsForEmailAndMember, language);
            } else {
                LOG.info("ORCID id found for {}. Sending notification.", email);
                sendNotification(email, orgName, salesforceId, allAssertionsForEmailAndMember, orcidId, language);
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

    private void markRequestCompleted(SendNotificationsRequest request) {
        LOG.info("Marking SendNotificationsRequest from user {} (salesforce ID {}) as complete", request.getEmail(), request.getSalesforceId());
        request.setDateCompleted(Instant.now());
        mailService.sendNotificationsSummary(internalUserServiceClient.getUser(request.getEmail()), request.getNotificationsSent(), request.getEmailsSent());
        sendNotificationsRequestRepository.save(request);
    }

    private void processRequest(SendNotificationsRequest request) {
        Iterator<String> emailsWithNotificationsRequested = assertionRepository.findDistinctEmailsWithNotificationRequested(request.getSalesforceId());
        Member member = internalMemberServiceClient.getMember(request.getSalesforceId());
        String orgName = member.getClientName();
        String language = member.getDefaultLanguage();
        emailsWithNotificationsRequested.forEachRemaining(e -> findAssertionsAndAttemptSend(e, orgName, language, request));
    }

    private void findAssertionsAndAttemptSend(String email, String orgName, String language, SendNotificationsRequest request) {
        List<Assertion> allAssertionsForEmailAndMember = assertionRepository.findByEmailAndSalesforceIdAndStatus(email, request.getSalesforceId(),
                AssertionStatus.NOTIFICATION_REQUESTED.name());
        try {
            String orcidId = orcidApiClient.getOrcidIdForEmail(email);
            if (orcidId == null) {
                LOG.info("No ORCID id found for {}. Sending email invitation instead.", email);
                sendEmailInvitation(email, orgName, request.getSalesforceId(), allAssertionsForEmailAndMember, language);
                request.setEmailsSent(request.getEmailsSent() + 1);
            } else {
                LOG.info("ORCID id found for {}. Sending notification.", email);
                sendNotification(email, orgName, request.getSalesforceId(), allAssertionsForEmailAndMember, orcidId, language);
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

    private void sendNotification(String email, String orgName, String salesforceId, List<Assertion> allAssertionsForEmailAndMember, String orcidId, String language)
            throws JAXBException, IOException {
        NotificationPermission notification = getPermissionLinkNotification(allAssertionsForEmailAndMember, email, salesforceId, orgName, language);
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

    private NotificationPermission getPermissionLinkNotification(List<Assertion> assertions, String email, String salesforceId, String orgName, String language) {
        Locale locale = LocaleUtils.getLocale(language);
        NotificationPermission notificationPermission = new NotificationPermission();
        notificationPermission.setNotificationIntro(messageSource.getMessage("assertion.notifications.introduction", null, locale));
        notificationPermission.setNotificationSubject(messageSource.getMessage("assertion.notifications.subject", new Object[]{orgName}, locale));
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

    private void sendEmailInvitation(String email, String orgName, String salesforceId, List<Assertion> allAssertionsForEmailAndMember, String language) {
        mailService.sendInvitationEmail(email, orgName, orcidRecordService.generateLinkForEmailAndSalesforceId(email, salesforceId), language);
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

}
