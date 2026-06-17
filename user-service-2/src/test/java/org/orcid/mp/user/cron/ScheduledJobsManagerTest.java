package org.orcid.mp.user.cron;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.user.service.UserService;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ScheduledJobsManagerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private ScheduledJobsManager scheduledJobsManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void sendActivationReminders_whenSchedulingEnabled_sendsActivationReminders() {
        ReflectionTestUtils.setField(scheduledJobsManager, "schedulingEnabled", true);
        scheduledJobsManager.sendActivationReminders();
        verify(userService, times(1)).sendActivationReminders();
    }

    @Test
    void sendActivationReminders_whenSchedulingDisabled_doesNotSendActivationReminders() {
        ReflectionTestUtils.setField(scheduledJobsManager, "schedulingEnabled", false);
        scheduledJobsManager.sendActivationReminders();
        verify(userService, never()).sendActivationReminders();
    }
}