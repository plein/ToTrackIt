package com.totrackit.task;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.repository.ProcessRepository;
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
 * Unit tests for DeadlineNotificationTask notification and retry semantics.
 */
@ExtendWith(MockitoExtension.class)
public class DeadlineNotificationTaskTest {

    @Mock
    private ProcessRepository processRepository;

    @Mock
    private WebhookNotificationService notificationService;

    private DeadlineNotificationTask task;

    @BeforeEach
    void setUp() {
        lenient().when(notificationService.isEnabled()).thenReturn(true);
        task = new DeadlineNotificationTask(processRepository, notificationService);
    }

    private ProcessEntity overdueProcess(Long id, String processId) {
        ProcessEntity entity = new ProcessEntity(processId, "test-process");
        entity.setId(id);
        entity.setDeadline(Instant.now().minusSeconds(3600));
        return entity;
    }

    @Test
    void testMarksProcessNotifiedAfterSuccessfulDelivery() {
        ProcessEntity process = overdueProcess(1L, "proc-1");
        when(processRepository.findOverdueUnnotified(any(Instant.class))).thenReturn(List.of(process));
        when(notificationService.sendDeadlineMissed(process)).thenReturn(true);

        task.notifyMissedDeadlines();

        verify(processRepository).markDeadlineNotified(eq(1L), any(Instant.class));
    }

    @Test
    void testDoesNotMarkNotifiedWhenDeliveryFails() {
        ProcessEntity process = overdueProcess(2L, "proc-2");
        when(processRepository.findOverdueUnnotified(any(Instant.class))).thenReturn(List.of(process));
        when(notificationService.sendDeadlineMissed(process)).thenReturn(false);

        task.notifyMissedDeadlines();

        verify(processRepository, never()).markDeadlineNotified(any(), any());
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
}
