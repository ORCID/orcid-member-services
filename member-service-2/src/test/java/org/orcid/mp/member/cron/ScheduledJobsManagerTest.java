package org.orcid.mp.member.cron;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.mp.member.service.SalesforceService;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ScheduledJobsManagerTest {

    @Mock
    private SalesforceService salesforceService;

    @InjectMocks
    private ScheduledJobsManager scheduledJobsManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testSyncMembers_whenSchedulingEnabled() throws IOException {
        ReflectionTestUtils.setField(scheduledJobsManager, "schedulingEnabled", true);
        scheduledJobsManager.syncMembers();
        verify(salesforceService).syncMembers();
    }

    @Test
    void testSyncMembers_whenSchedulingDisabled() throws IOException {
        ReflectionTestUtils.setField(scheduledJobsManager, "schedulingEnabled", false);
        scheduledJobsManager.syncMembers();
        verify(salesforceService, never()).syncMembers();
    }
}