package org.orcid.sync;

import javax.xml.bind.JAXBException;

import org.orcid.service.AssertionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@EnableScheduling
public class OrcidSyncManager {

    private final Logger log = LoggerFactory.getLogger(OrcidSyncManager.class);

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
}
