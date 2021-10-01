package org.orcid.memberportal.service.user.scheduledTasks;

import javax.xml.bind.JAXBException;

import org.orcid.memberportal.service.user.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Component
@EnableScheduling
public class UserScheduledTasks {
    
    private final Logger log = LoggerFactory.getLogger(UserScheduledTasks.class);
    
    @Autowired
    private UserService userService;
    
    @Scheduled(fixedDelayString = "${application.cron.sendActivationReminders}")
    @SchedulerLock(name = "sendActivationReminders", lockAtMostFor = "20m", lockAtLeastFor = "10m")
    public void sendActivationReminders() throws JAXBException {
        log.info("Running cron to send activation reminders");
        userService.sendActivationReminders();
        log.info("Reminders sent");
    }
    
}
