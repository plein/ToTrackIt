package com.totrackit.service;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.MetricsService;
import com.totrackit.util.ProcessMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify ProcessService functionality and business logic.
 * These tests verify the service methods work correctly with the entity model.
 */
class ProcessServiceFunctionalityTest {
    
    @Mock
    private ProcessRepository processRepository;
    
    @Mock
    private ProcessMapper processMapper;
    
    @Mock
    private MetricsService metricsService;
    
    private ProcessService processService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processService = new ProcessService(processRepository, processMapper, metricsService);
    }
    
    @Test
    void calculateDeadlineStatus_OnTrack() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setDeadline(Instant.now().plusSeconds(3600)); // 1 hour from now
        entity.setStatus(ProcessStatus.ACTIVE);
        
        DeadlineStatus status = processService.calculateDeadlineStatus(entity);
        assertEquals(DeadlineStatus.ON_TRACK, status);
    }
    
    @Test
    void calculateDeadlineStatus_Missed() {
        ProcessEntity entity = new ProcessEntity("missed-id", "missed-process");
        entity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        entity.setStatus(ProcessStatus.ACTIVE);
        
        DeadlineStatus status = processService.calculateDeadlineStatus(entity);
        assertEquals(DeadlineStatus.MISSED, status);
    }
    
    @Test
    void calculateDeadlineStatus_CompletedOnTime() {
        ProcessEntity entity = new ProcessEntity("completed-id", "completed-process");
        Instant deadline = Instant.now().plusSeconds(3600);
        entity.setDeadline(deadline);
        entity.setStatus(ProcessStatus.COMPLETED);
        entity.setCompletedAt(deadline.minusSeconds(300)); // Completed 5 minutes early
        
        DeadlineStatus status = processService.calculateDeadlineStatus(entity);
        assertEquals(DeadlineStatus.COMPLETED_ON_TIME, status);
    }
    
    @Test
    void calculateDeadlineStatus_CompletedLate() {
        ProcessEntity entity = new ProcessEntity("late-id", "late-process");
        Instant deadline = Instant.now().minusSeconds(3600);
        entity.setDeadline(deadline);
        entity.setStatus(ProcessStatus.COMPLETED);
        entity.setCompletedAt(deadline.plusSeconds(300)); // Completed 5 minutes late
        
        DeadlineStatus status = processService.calculateDeadlineStatus(entity);
        assertEquals(DeadlineStatus.COMPLETED_LATE, status);
    }
    
    @Test
    void calculateDuration_ActiveProcess() {
        ProcessEntity entity = new ProcessEntity("duration-test", "duration-process");
        Instant startTime = Instant.now().minusSeconds(300); // Started 5 minutes ago
        entity.setStartedAt(startTime);
        entity.setStatus(ProcessStatus.ACTIVE);
        
        Long duration = processService.calculateDuration(entity);
        assertNotNull(duration);
        assertTrue(duration >= 300); // At least 5 minutes
        assertTrue(duration <= 310); // Allow some tolerance for test execution time
    }
    
    @Test
    void calculateDuration_CompletedProcess() {
        ProcessEntity entity = new ProcessEntity("duration-test", "duration-process");
        Instant startTime = Instant.now().minusSeconds(600); // Started 10 minutes ago
        Instant completionTime = startTime.plusSeconds(300); // Completed after 5 minutes
        entity.setStartedAt(startTime);
        entity.setCompletedAt(completionTime);
        entity.setStatus(ProcessStatus.COMPLETED);
        
        Long duration = processService.calculateDuration(entity);
        assertEquals(300L, duration); // Exactly 5 minutes
    }
    
    @Test
    void isProcessOverdue_OverdueActiveProcess() {
        ProcessEntity entity = new ProcessEntity("overdue-id", "overdue-process");
        entity.setStatus(ProcessStatus.ACTIVE);
        entity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        assertTrue(processService.isProcessOverdue(entity));
    }
    
    @Test
    void isProcessOverdue_OnTrackProcess() {
        ProcessEntity entity = new ProcessEntity("ontrack-id", "ontrack-process");
        entity.setStatus(ProcessStatus.ACTIVE);
        entity.setDeadline(Instant.now().plusSeconds(3600)); // 1 hour from now
        
        assertFalse(processService.isProcessOverdue(entity));
    }
    
    @Test
    void isProcessOverdue_CompletedProcess() {
        ProcessEntity entity = new ProcessEntity("completed-id", "completed-process");
        entity.setStatus(ProcessStatus.COMPLETED);
        entity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        assertFalse(processService.isProcessOverdue(entity)); // Completed processes are never overdue
    }
    
    @Test
    void isProcessOverdue_NoDeadline() {
        ProcessEntity entity = new ProcessEntity("no-deadline-id", "no-deadline-process");
        entity.setStatus(ProcessStatus.ACTIVE);
        // No deadline set
        
        assertFalse(processService.isProcessOverdue(entity));
    }
}