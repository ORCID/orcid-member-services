package org.orcid.mp.member.cron;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.orcid.mp.member.service.SalesforceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@EnableScheduling
public class ScheduledJobsManager {

    @Autowired
    private SalesforceService salesforceService;

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledJobsManager.class);

    @Scheduled(cron = "${application.cron.syncSalesforceMembers}")
    @SchedulerLock(name = "syncMembers", lockAtMostFor = "60m", lockAtLeastFor = "10m")
    public void syncMembers() throws IOException {
        LOG.info("Running cron to sync salesforce members");
        salesforceService.syncMembers();
        LOG.info("Salesforce sync complete");
    }

}
