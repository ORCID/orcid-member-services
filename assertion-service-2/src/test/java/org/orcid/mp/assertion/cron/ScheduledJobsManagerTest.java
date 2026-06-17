package org.orcid.mp.assertion.cron;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.assertion.service.AssertionService;
import org.orcid.mp.assertion.service.CsvReportService;
import org.orcid.mp.assertion.service.NotificationService;
import org.orcid.mp.assertion.service.StoredFileService;
import org.springframework.test.util.ReflectionTestUtils;

import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ScheduledJobsManagerTest {

    @Mock
    private AssertionService assertionService;

    @Mock
    private StoredFileService storedFileService;

    @Mock
    private CsvReportService csvReportService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ScheduledJobsManager scheduledJobsManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testSyncAffiliationsWhenSchedulingEnabled() throws JAXBException {
        enableScheduling();

        scheduledJobsManager.syncAffiliations();

        verify(assertionService).postAssertionsToOrcid();
        verify(assertionService).putAssertionsInOrcid();
    }

    @Test
    void testSyncAffiliationsWhenSchedulingDisabled() throws JAXBException {
        disableScheduling();

        scheduledJobsManager.syncAffiliations();

        verify(assertionService, never()).postAssertionsToOrcid();
        verify(assertionService, never()).putAssertionsInOrcid();
        verifyNoInteractions(storedFileService, csvReportService, notificationService);
    }

    @Test
    void testGenerateMemberAssertionStatsWhenSchedulingEnabled() throws IOException {
        enableScheduling();

        scheduledJobsManager.generateMemberAssertionStats();

        verify(assertionService).generateAndSendMemberAssertionStats();
    }

    @Test
    void testGenerateMemberAssertionStatsWhenSchedulingDisabled() throws IOException {
        disableScheduling();

        scheduledJobsManager.generateMemberAssertionStats();

        verify(assertionService, never()).generateAndSendMemberAssertionStats();
        verifyNoInteractions(storedFileService, csvReportService, notificationService);
    }

    @Test
    void testProcessAssertionUploadsWhenSchedulingEnabled() throws IOException {
        enableScheduling();

        scheduledJobsManager.processAssertionUploads();

        verify(assertionService).processAssertionUploads();
    }

    @Test
    void testProcessAssertionUploadsWhenSchedulingDisabled() throws IOException {
        disableScheduling();

        scheduledJobsManager.processAssertionUploads();

        verify(assertionService, never()).processAssertionUploads();
        verifyNoInteractions(storedFileService, csvReportService, notificationService);
    }

    @Test
    void testRemoveStoredFilesWhenSchedulingEnabled() throws IOException {
        enableScheduling();

        scheduledJobsManager.removeStoredFiles();

        verify(storedFileService).removeStoredFiles();
    }

    @Test
    void testRemoveStoredFilesWhenSchedulingDisabled() throws IOException {
        disableScheduling();

        scheduledJobsManager.removeStoredFiles();

        verify(storedFileService, never()).removeStoredFiles();
        verifyNoInteractions(assertionService, csvReportService, notificationService);
    }

    @Test
    void testSendCSVReportsWhenSchedulingEnabled() throws IOException {
        enableScheduling();

        scheduledJobsManager.sendCSVReports();

        verify(csvReportService).processCsvReports();
    }

    @Test
    void testSendCSVReportsWhenSchedulingDisabled() throws IOException {
        disableScheduling();

        scheduledJobsManager.sendCSVReports();

        verify(csvReportService, never()).processCsvReports();
        verifyNoInteractions(assertionService, storedFileService, notificationService);
    }

    @Test
    void testSendPermissionLinkNotificationsWhenSchedulingEnabled() throws IOException {
        enableScheduling();

        scheduledJobsManager.sendPermissionLinkNotifications();

        verify(notificationService).sendPermissionLinkNotifications();
    }

    @Test
    void testSendPermissionLinkNotificationsWhenSchedulingDisabled() throws IOException {
        disableScheduling();

        scheduledJobsManager.sendPermissionLinkNotifications();

        verify(notificationService, never()).sendPermissionLinkNotifications();
        verifyNoInteractions(assertionService, storedFileService, csvReportService);
    }

    @Test
    void testResendNotificationsWhenSchedulingEnabled() throws IOException {
        enableScheduling();

        scheduledJobsManager.resendNotifications();

        verify(notificationService).resendNotifications();
    }

    @Test
    void testResendNotificationsWhenSchedulingDisabled() throws IOException {
        disableScheduling();

        scheduledJobsManager.resendNotifications();

        verify(notificationService, never()).resendNotifications();
        verifyNoInteractions(assertionService, storedFileService, csvReportService);
    }

    private void enableScheduling() {
        ReflectionTestUtils.setField(scheduledJobsManager, "schedulingEnabled", true);
    }

    private void disableScheduling() {
        ReflectionTestUtils.setField(scheduledJobsManager, "schedulingEnabled", false);
    }
}