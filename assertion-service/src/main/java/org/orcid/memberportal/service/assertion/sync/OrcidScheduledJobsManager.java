package org.orcid.memberportal.service.assertion.sync;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.orcid.memberportal.service.assertion.services.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@EnableScheduling
public class OrcidScheduledJobsManager {

    private final Logger log = LoggerFactory.getLogger(OrcidScheduledJobsManager.class);

    @Autowired
    private AssertionService assertionsService;

    @Scheduled(fixedDelayString = "${application.cron.syncAffiliations}")
    @SchedulerLock(name = "syncAffiliations", lockAtMostFor = "20m", lockAtLeastFor = "2m")
    public void syncAffiliations() throws JAXBException {
        log.info("Running cron to sync assertions with registry");
        assertionsService.postAssertionsToOrcid();
        assertionsService.putAssertionsInOrcid();
        log.info("Sync complete");
    }
    
    @Scheduled(cron = "${application.cron.generateMemberAssertionStats")
    @SchedulerLock(name = "generateMemberAssertionStats", lockAtMostFor = "60m", lockAtLeastFor = "10m")
    public void generateMemberAssertionStats() throws IOException  {
        assertionsService.generateAndSendMemberAssertionStats();
    }
}
