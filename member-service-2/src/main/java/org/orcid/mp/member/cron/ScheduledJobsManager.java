package org.orcid.mp.member.cron;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.orcid.mp.member.service.SalesforceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "45m")
public class ScheduledJobsManager {

    @Autowired
    private SalesforceService salesforceService;

    @Value("${application.cron.enabled}")
    private boolean schedulingEnabled;

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobsManager.class);

    @Scheduled(cron = "${application.cron.syncSalesforceMembers}")
    @SchedulerLock(name = "syncMembers", lockAtMostFor = "60m", lockAtLeastFor = "10m")
    public void syncMembers() throws IOException {
        if (!schedulingEnabled) {
            LOG.debug("Scheduling is disabled, not syncing members");
            return;
        }
        LOG.info("Running cron to sync salesforce members");
        salesforceService.syncMembers();
        LOG.info("Salesforce sync complete");
    }

}
