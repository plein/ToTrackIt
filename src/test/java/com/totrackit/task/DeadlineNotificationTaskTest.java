package com.totrackit.task;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.AdvisoryLockService;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DeadlineNotificationTask notification, retry, batching and
 * metrics semantics. Threshold crossing is computed in SQL now, so the
 * repository mock returns only rows that are already due a warning.
 */
@ExtendWith(MockitoExtension.class)
public class DeadlineNotificationTaskTest {

    private static final int BATCH = 500;

    @Mock
    private ProcessRepository processRepository;

    @Mock
    private MetricsService metricsService;

    @Mock
    private AdvisoryLockService advisoryLockService;

    @Mock
    private WebhookNotificationService notificationService;

    private DeadlineNotificationTask task;

    @BeforeEach
    void setUp() {
        lenient().when(notificationService.isEnabled()).thenReturn(true);
        // The scan runs only while holding the advisory lock; execute inline.
        lenient().when(advisoryLockService.runExclusive(anyLong(), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    ((Runnable) invocation.getArgument(1)).run();
                    return true;
                });
        lenient().when(processRepository.findApproachingUnwarned(any(Instant.class), anyDouble(), anyInt()))
                .thenReturn(Collections.emptyList());
        lenient().when(processRepository.findOverdueUnnotified(any(Instant.class), anyInt()))
                .thenReturn(Collections.emptyList());
        lenient().when(processRepository.countOverdueUnnotified(any(Instant.class))).thenReturn(0L);
        lenient().when(processRepository.countApproachingUnwarned(any(Instant.class), anyDouble())).thenReturn(0L);
        task = newTask(notificationService, 0.75);
    }

    private DeadlineNotificationTask newTask(WebhookNotificationService webhook, double threshold) {
        return new DeadlineNotificationTask(processRepository, metricsService, advisoryLockService,
                webhook, threshold, BATCH);
    }

    private ProcessEntity overdueProcess(Long id, String processId) {
        ProcessEntity entity = new ProcessEntity(processId, "test-process");
        entity.setId(id);
        entity.setDeadline(Instant.now().minusSeconds(3600));
        return entity;
    }

    /** Active process past the warning threshold (as the SQL query would return). */
    private ProcessEntity atRiskProcess(Long id, String processId) {
        ProcessEntity entity = new ProcessEntity(processId, "test-process");
        entity.setId(id);
        long budget = 1000;
        entity.setStartedAt(Instant.now().minusSeconds(800));
        entity.setDeadline(entity.getStartedAt().plusSeconds(budget));
        return entity;
    }

    @Test
    void testMarksProcessNotifiedAfterSuccessfulDelivery() {
        ProcessEntity process = overdueProcess(1L, "proc-1");
        when(processRepository.findOverdueUnnotified(any(Instant.class), anyInt())).thenReturn(List.of(process));
        when(notificationService.sendDeadlineMissed(process)).thenReturn(true);

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineNotifiedBatch(eq(List.of(1L)), any(Instant.class));
        verify(metricsService).recordDeadlineMissed("test-process");
    }

    @Test
    void testDoesNotMarkNotifiedWhenDeliveryFails() {
        ProcessEntity process = overdueProcess(2L, "proc-2");
        when(processRepository.findOverdueUnnotified(any(Instant.class), anyInt())).thenReturn(List.of(process));
        when(notificationService.sendDeadlineMissed(process)).thenReturn(false);

        task.notifyMissedDeadlines();

        verify(processRepository, never()).markDeadlineNotifiedBatch(any(), any());
        verify(metricsService, never()).recordDeadlineMissed(any());
    }

    @Test
    void testPartialDeliveryOnlyMarksDeliveredProcesses() {
        ProcessEntity delivered = overdueProcess(1L, "proc-1");
        ProcessEntity failed = overdueProcess(2L, "proc-2");
        when(processRepository.findOverdueUnnotified(any(Instant.class), anyInt()))
                .thenReturn(List.of(delivered, failed));
        when(notificationService.sendDeadlineMissed(delivered)).thenReturn(true);
        when(notificationService.sendDeadlineMissed(failed)).thenReturn(false);

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineNotifiedBatch(eq(List.of(1L)), any(Instant.class));
    }

    @Test
    void testNoScanWorkWhenNothingOverdue() {
        when(processRepository.findOverdueUnnotified(any(Instant.class), anyInt()))
                .thenReturn(Collections.emptyList());

        task.notifyMissedDeadlines();

        verify(notificationService, never()).sendDeadlineMissed(any());
        verify(processRepository, never()).markDeadlineNotifiedBatch(any(), any());
    }

    @Test
    void testScanRunsWithoutWebhookAndRecordsMetric() {
        // No webhook configured at all: scanner still processes breaches so the
        // deadline-missed metric works for Prometheus/Datadog-only deployments.
        task = newTask(null, 0.75);
        ProcessEntity process = overdueProcess(3L, "proc-3");
        when(processRepository.findOverdueUnnotified(any(Instant.class), anyInt())).thenReturn(List.of(process));

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineNotifiedBatch(eq(List.of(3L)), any(Instant.class));
        verify(metricsService).recordDeadlineMissed("test-process");
    }

    @Test
    void testScanRunsWhenWebhookConfiguredButDisabled() {
        // Property set but blank: bean exists, isEnabled() is false
        when(notificationService.isEnabled()).thenReturn(false);
        ProcessEntity process = overdueProcess(4L, "proc-4");
        when(processRepository.findOverdueUnnotified(any(Instant.class), anyInt())).thenReturn(List.of(process));

        task.notifyMissedDeadlines();

        verify(notificationService, never()).sendDeadlineMissed(any());
        verify(processRepository).markDeadlineNotifiedBatch(eq(List.of(4L)), any(Instant.class));
        verify(metricsService).recordDeadlineMissed("test-process");
    }

    @Test
    void testWarningFiredForRowsPastThreshold() {
        ProcessEntity atRisk = atRiskProcess(10L, "proc-w1");
        when(processRepository.findApproachingUnwarned(any(Instant.class), anyDouble(), anyInt()))
                .thenReturn(List.of(atRisk));
        when(notificationService.sendDeadlineWarning(eq(atRisk), anyLong())).thenReturn(true);

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineWarnedBatch(eq(List.of(10L)), any(Instant.class));
        verify(metricsService).recordDeadlineWarning("test-process");
    }

    @Test
    void testWarningNotMarkedWhenDeliveryFails() {
        ProcessEntity atRisk = atRiskProcess(12L, "proc-w3");
        when(processRepository.findApproachingUnwarned(any(Instant.class), anyDouble(), anyInt()))
                .thenReturn(List.of(atRisk));
        when(notificationService.sendDeadlineWarning(eq(atRisk), anyLong())).thenReturn(false);

        task.notifyMissedDeadlines();

        verify(processRepository, never()).markDeadlineWarnedBatch(any(), any());
        verify(metricsService, never()).recordDeadlineWarning(any());
    }

    @Test
    void testWarningWithoutWebhookMarksAndRecordsMetric() {
        task = newTask(null, 0.75);
        ProcessEntity atRisk = atRiskProcess(13L, "proc-w4");
        when(processRepository.findApproachingUnwarned(any(Instant.class), anyDouble(), anyInt()))
                .thenReturn(List.of(atRisk));

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineWarnedBatch(eq(List.of(13L)), any(Instant.class));
        verify(metricsService).recordDeadlineWarning("test-process");
    }

    @Test
    void testWarningsDisabledByThreshold() {
        task = newTask(notificationService, 0.0);

        task.notifyMissedDeadlines();

        verify(processRepository, never()).findApproachingUnwarned(any(Instant.class), anyDouble(), anyInt());
    }

    @Test
    void testSkipsCycleWhenLockHeldElsewhere() {
        when(advisoryLockService.runExclusive(anyLong(), any(Runnable.class))).thenReturn(false);

        task.notifyMissedDeadlines();

        verify(processRepository, never()).findOverdueUnnotified(any(Instant.class), anyInt());
        verify(processRepository, never()).findApproachingUnwarned(any(Instant.class), anyDouble(), anyInt());
    }

    @Test
    void testAbortsPassAfterConsecutiveDeliveryFailures() {
        List<ProcessEntity> overdue = List.of(
                overdueProcess(1L, "p1"), overdueProcess(2L, "p2"), overdueProcess(3L, "p3"),
                overdueProcess(4L, "p4"), overdueProcess(5L, "p5"), overdueProcess(6L, "p6"),
                overdueProcess(7L, "p7"));
        when(processRepository.findOverdueUnnotified(any(Instant.class), anyInt())).thenReturn(overdue);
        when(notificationService.sendDeadlineMissed(any())).thenReturn(false);

        task.notifyMissedDeadlines();

        // Circuit-break: the pass stops at 5 consecutive failures instead of
        // hammering a dead endpoint for the whole batch.
        verify(notificationService, times(5)).sendDeadlineMissed(any());
        verify(processRepository, never()).markDeadlineNotifiedBatch(any(), any());
    }

    @Test
    void testBacklogGaugesPublishedEachCycle() {
        when(processRepository.countOverdueUnnotified(any(Instant.class))).thenReturn(7L);
        when(processRepository.countApproachingUnwarned(any(Instant.class), anyDouble())).thenReturn(3L);

        task.notifyMissedDeadlines();

        verify(metricsService).updateNotificationBacklog(7L, 3L);
    }

    @Test
    void testConsecutiveFailureCounterResetsOnSuccess() {
        List<ProcessEntity> overdue = List.of(
                overdueProcess(1L, "p1"), overdueProcess(2L, "p2"), overdueProcess(3L, "p3"),
                overdueProcess(4L, "p4"), overdueProcess(5L, "p5"), overdueProcess(6L, "p6"));
        when(processRepository.findOverdueUnnotified(any(Instant.class), anyInt())).thenReturn(overdue);
        // Alternating failure/success never reaches 5 consecutive failures
        when(notificationService.sendDeadlineMissed(any()))
                .thenReturn(false, true, false, true, false, true);

        task.notifyMissedDeadlines();

        verify(notificationService, times(6)).sendDeadlineMissed(any());
        verify(processRepository).markDeadlineNotifiedBatch(eq(List.of(2L, 4L, 6L)), any(Instant.class));
        verify(metricsService, atMost(3)).recordDeadlineMissed(any());
    }
}
