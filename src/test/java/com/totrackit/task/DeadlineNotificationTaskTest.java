package com.totrackit.task;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.MetricsService;
import com.totrackit.service.WebhookNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DeadlineNotificationTask notification, retry, and metrics semantics.
 */
@ExtendWith(MockitoExtension.class)
public class DeadlineNotificationTaskTest {

    @Mock
    private ProcessRepository processRepository;

    @Mock
    private MetricsService metricsService;

    @Mock
    private WebhookNotificationService notificationService;

    private DeadlineNotificationTask task;

    @BeforeEach
    void setUp() {
        lenient().when(notificationService.isEnabled()).thenReturn(true);
        lenient().when(processRepository.findApproachingUnwarned(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        lenient().when(processRepository.findOverdueUnnotified(any(Instant.class)))
                .thenReturn(Collections.emptyList());
        task = new DeadlineNotificationTask(processRepository, metricsService, notificationService, 0.75);
    }

    private ProcessEntity overdueProcess(Long id, String processId) {
        ProcessEntity entity = new ProcessEntity(processId, "test-process");
        entity.setId(id);
        entity.setDeadline(Instant.now().minusSeconds(3600));
        return entity;
    }

    /** Active process at the given fraction of its deadline budget. */
    private ProcessEntity activeProcessAtBudgetFraction(Long id, String processId, double fraction) {
        ProcessEntity entity = new ProcessEntity(processId, "test-process");
        entity.setId(id);
        long budget = 1000;
        entity.setStartedAt(Instant.now().minusSeconds((long) (budget * fraction)));
        entity.setDeadline(entity.getStartedAt().plusSeconds(budget));
        return entity;
    }

    @Test
    void testMarksProcessNotifiedAfterSuccessfulDelivery() {
        ProcessEntity process = overdueProcess(1L, "proc-1");
        when(processRepository.findOverdueUnnotified(any(Instant.class))).thenReturn(List.of(process));
        when(notificationService.sendDeadlineMissed(process)).thenReturn(true);

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineNotified(eq(1L), any(Instant.class));
        verify(metricsService).recordDeadlineMissed("test-process");
    }

    @Test
    void testDoesNotMarkNotifiedWhenDeliveryFails() {
        ProcessEntity process = overdueProcess(2L, "proc-2");
        when(processRepository.findOverdueUnnotified(any(Instant.class))).thenReturn(List.of(process));
        when(notificationService.sendDeadlineMissed(process)).thenReturn(false);

        task.notifyMissedDeadlines();

        verify(processRepository, never()).markDeadlineNotified(any(), any());
        verify(metricsService, never()).recordDeadlineMissed(any());
    }

    @Test
    void testPartialDeliveryOnlyMarksDeliveredProcesses() {
        ProcessEntity delivered = overdueProcess(1L, "proc-1");
        ProcessEntity failed = overdueProcess(2L, "proc-2");
        when(processRepository.findOverdueUnnotified(any(Instant.class)))
                .thenReturn(List.of(delivered, failed));
        when(notificationService.sendDeadlineMissed(delivered)).thenReturn(true);
        when(notificationService.sendDeadlineMissed(failed)).thenReturn(false);

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineNotified(eq(1L), any(Instant.class));
        verify(processRepository, never()).markDeadlineNotified(eq(2L), any(Instant.class));
    }

    @Test
    void testNoScanWorkWhenNothingOverdue() {
        when(processRepository.findOverdueUnnotified(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        task.notifyMissedDeadlines();

        verify(notificationService, never()).sendDeadlineMissed(any());
        verify(processRepository, never()).markDeadlineNotified(any(), any());
    }

    @Test
    void testScanRunsWithoutWebhookAndRecordsMetric() {
        // No webhook configured at all: scanner still processes breaches so the
        // deadline-missed metric works for Prometheus/Datadog-only deployments.
        task = new DeadlineNotificationTask(processRepository, metricsService, null, 0.75);
        ProcessEntity process = overdueProcess(3L, "proc-3");
        when(processRepository.findOverdueUnnotified(any(Instant.class))).thenReturn(List.of(process));

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineNotified(eq(3L), any(Instant.class));
        verify(metricsService).recordDeadlineMissed("test-process");
    }

    @Test
    void testScanRunsWhenWebhookConfiguredButDisabled() {
        // Property set but blank: bean exists, isEnabled() is false
        when(notificationService.isEnabled()).thenReturn(false);
        ProcessEntity process = overdueProcess(4L, "proc-4");
        when(processRepository.findOverdueUnnotified(any(Instant.class))).thenReturn(List.of(process));

        task.notifyMissedDeadlines();

        verify(notificationService, never()).sendDeadlineMissed(any());
        verify(processRepository).markDeadlineNotified(eq(4L), any(Instant.class));
        verify(metricsService).recordDeadlineMissed("test-process");
    }

    @Test
    void testWarningFiredWhenThresholdCrossed() {
        ProcessEntity atRisk = activeProcessAtBudgetFraction(10L, "proc-w1", 0.8);
        when(processRepository.findApproachingUnwarned(any(Instant.class))).thenReturn(List.of(atRisk));
        when(notificationService.sendDeadlineWarning(eq(atRisk), org.mockito.ArgumentMatchers.anyLong())).thenReturn(true);

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineWarned(eq(10L), any(Instant.class));
        verify(metricsService).recordDeadlineWarning("test-process");
    }

    @Test
    void testNoWarningBelowThreshold() {
        ProcessEntity early = activeProcessAtBudgetFraction(11L, "proc-w2", 0.5);
        when(processRepository.findApproachingUnwarned(any(Instant.class))).thenReturn(List.of(early));

        task.notifyMissedDeadlines();

        verify(notificationService, never()).sendDeadlineWarning(any(), org.mockito.ArgumentMatchers.anyLong());
        verify(processRepository, never()).markDeadlineWarned(any(), any());
        verify(metricsService, never()).recordDeadlineWarning(any());
    }

    @Test
    void testWarningNotMarkedWhenDeliveryFails() {
        ProcessEntity atRisk = activeProcessAtBudgetFraction(12L, "proc-w3", 0.9);
        when(processRepository.findApproachingUnwarned(any(Instant.class))).thenReturn(List.of(atRisk));
        when(notificationService.sendDeadlineWarning(eq(atRisk), org.mockito.ArgumentMatchers.anyLong())).thenReturn(false);

        task.notifyMissedDeadlines();

        verify(processRepository, never()).markDeadlineWarned(any(), any());
        verify(metricsService, never()).recordDeadlineWarning(any());
    }

    @Test
    void testWarningWithoutWebhookMarksAndRecordsMetric() {
        task = new DeadlineNotificationTask(processRepository, metricsService, null, 0.75);
        ProcessEntity atRisk = activeProcessAtBudgetFraction(13L, "proc-w4", 0.8);
        when(processRepository.findApproachingUnwarned(any(Instant.class))).thenReturn(List.of(atRisk));

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineWarned(eq(13L), any(Instant.class));
        verify(metricsService).recordDeadlineWarning("test-process");
    }

    @Test
    void testWarningsDisabledByThreshold() {
        task = new DeadlineNotificationTask(processRepository, metricsService, notificationService, 0.0);

        task.notifyMissedDeadlines();

        verify(processRepository, never()).findApproachingUnwarned(any(Instant.class));
    }
}
