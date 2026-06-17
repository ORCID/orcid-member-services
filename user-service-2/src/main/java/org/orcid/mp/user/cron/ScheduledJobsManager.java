package org.orcid.mp.user.cron;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.orcid.mp.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "15m")
public class ScheduledJobsManager {

    private final Logger LOG = LoggerFactory.getLogger(ScheduledJobsManager.class);

    @Autowired
    private UserService userService;

    @Value("${application.cron.enabled}")
    private boolean schedulingEnabled;

    @Scheduled(fixedDelayString = "${application.sendActivationRemindersDelay}")
    @SchedulerLock(name = "sendActivationReminders", lockAtMostFor = "20m", lockAtLeastFor = "10m")
    public void sendActivationReminders() {
        if (!schedulingEnabled) {
            LOG.debug("Scheduling disabled, not sending activation reminders");
            return;
        }
        LOG.info("Running cron to send activation reminders");
        userService.sendActivationReminders();
        LOG.info("Reminders sent");
    }

}
