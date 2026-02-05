package org.orcid.mp.assertion.cron;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.orcid.mp.assertion.service.AssertionService;
import org.orcid.mp.assertion.service.CsvReportService;
import org.orcid.mp.assertion.service.NotificationService;
import org.orcid.mp.assertion.service.StoredFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.io.IOException;

@Component
@EnableScheduling
public class ScheduledJobsManager {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobsManager.class);

    @Autowired
    private AssertionService assertionService;

    @Autowired
    private StoredFileService storedFileService;

    @Autowired
    private CsvReportService csvReportService;

    @Autowired
    private NotificationService notificationService;

    @Scheduled(initialDelay = 90000, fixedDelayString = "${application.cron.syncAffiliationsDelay}")
    @SchedulerLock(name = "syncAffiliations", lockAtMostFor = "20m", lockAtLeastFor = "2m")
    public void syncAffiliations() throws JAXBException {
        LOG.info("Running cron to sync assertions with registry");
        assertionService.postAssertionsToOrcid();
        assertionService.putAssertionsInOrcid();
        LOG.info("Sync complete");
    }

    @Scheduled(cron = "${application.cron.generateMemberAssertionStatsCron}")
    @SchedulerLock(name = "generateMemberAssertionStats", lockAtMostFor = "60m", lockAtLeastFor = "10m")
    public void generateMemberAssertionStats() throws IOException {
        LOG.info("Running cron to generate member assertion stats");
        assertionService.generateAndSendMemberAssertionStats();
        LOG.info("Stats generation complete");
    }

    @Scheduled(initialDelay = 90000, fixedDelayString = "${application.cron.processAssertionUploadsDelay}")
    @SchedulerLock(name = "processAssertionUploads", lockAtMostFor = "60m", lockAtLeastFor = "2m")
    public void processAssertionUploads() throws IOException  {
        LOG.info("Running cron to process assertion uploads");
        assertionService.processAssertionUploads();
        LOG.info("Assertion uploads processed");
    }

    @Scheduled(initialDelay = 90000, fixedDelayString = "${application.cron.removeStoredFilesDelay}")
    @SchedulerLock(name = "removeStoredFiles", lockAtMostFor = "60m", lockAtLeastFor = "2m")
    public void removeStoredFiles() throws IOException  {
        LOG.info("Running cron to remove old files");
        storedFileService.removeStoredFiles();
        LOG.info("Old files removed");
    }

    @Scheduled(initialDelay = 90000, fixedDelayString = "${application.cron.processCsvReportsDelay}")
    @SchedulerLock(name = "processCsvReports", lockAtMostFor = "60m", lockAtLeastFor = "2m")
    public void sendCSVReports() throws IOException  {
        LOG.info("Running cron to process CSV reports");
        csvReportService.processCsvReports();
        LOG.info("CSV reports processed");
    }

    @Scheduled(initialDelay = 90000, fixedDelayString = "${application.cron.sendPermissionLinkNotificationsDelay}")
    @SchedulerLock(name = "sendPermissionLinkNotifications", lockAtMostFor = "60m", lockAtLeastFor = "2m")
    public void sendPermissionLinkNotifications() throws IOException  {
        LOG.info("Running cron to send permission link notifications");
        notificationService.sendPermissionLinkNotifications();
        LOG.info("Permission link notifications sent");
    }

    @Scheduled(cron = "${application.cron.resendNotificationsCron}")
    @SchedulerLock(name = "resendNotifications", lockAtMostFor = "60m", lockAtLeastFor = "10m")
    public void resendNotifications() throws IOException  {
        LOG.info("Running cron to resend notifications");
        notificationService.resendNotifications();
        LOG.info("Notifications resent");
    }


}
