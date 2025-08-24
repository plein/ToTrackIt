package com.totrackit.service;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.util.ProcessMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ProcessService business logic methods.
 */
class ProcessServiceBusinessLogicTest {
    
    @Mock
    private ProcessRepository processRepository;
    
    @Mock
    private ProcessMapper processMapper;
    
    private ProcessService processService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processService = new ProcessService(processRepository, processMapper);
    }
    
    @Test
    void calculateDeadlineStatus_ActiveOnTrack() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStatus(ProcessStatus.ACTIVE);
        entity.setDeadline(Instant.now().plusSeconds(3600)); // 1 hour from now
        
        DeadlineStatus status = processService.calculateDeadlineStatus(entity);
        
        assertEquals(DeadlineStatus.ON_TRACK, status);
    }
    
    @Test
    void calculateDeadlineStatus_ActiveMissed() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStatus(ProcessStatus.ACTIVE);
        entity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        DeadlineStatus status = processService.calculateDeadlineStatus(entity);
        
        assertEquals(DeadlineStatus.MISSED, status);
    }
    
    @Test
    void calculateDeadlineStatus_CompletedOnTime() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStatus(ProcessStatus.COMPLETED);
        Instant deadline = Instant.now().plusSeconds(3600);
        entity.setDeadline(deadline);
        entity.setCompletedAt(deadline.minusSeconds(300)); // Completed 5 minutes early
        
        DeadlineStatus status = processService.calculateDeadlineStatus(entity);
        
        assertEquals(DeadlineStatus.COMPLETED_ON_TIME, status);
    }
    
    @Test
    void calculateDeadlineStatus_CompletedLate() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStatus(ProcessStatus.COMPLETED);
        Instant deadline = Instant.now().minusSeconds(3600);
        entity.setDeadline(deadline);
        entity.setCompletedAt(deadline.plusSeconds(300)); // Completed 5 minutes late
        
        DeadlineStatus status = processService.calculateDeadlineStatus(entity);
        
        assertEquals(DeadlineStatus.COMPLETED_LATE, status);
    }
    
    @Test
    void calculateDeadlineStatus_NoDeadline() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStatus(ProcessStatus.ACTIVE);
        // No deadline set
        
        DeadlineStatus status = processService.calculateDeadlineStatus(entity);
        
        assertNull(status);
    }
    
    @Test
    void calculateDeadlineStatus_NullEntity() {
        DeadlineStatus status = processService.calculateDeadlineStatus(null);
        
        assertNull(status);
    }
    
    @Test
    void calculateDuration_ActiveProcess() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
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
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        Instant startTime = Instant.now().minusSeconds(600);
        Instant completionTime = startTime.plusSeconds(300); // Completed after 5 minutes
        entity.setStartedAt(startTime);
        entity.setCompletedAt(completionTime);
        entity.setStatus(ProcessStatus.COMPLETED);
        
        Long duration = processService.calculateDuration(entity);
        
        assertEquals(300L, duration);
    }
    
    @Test
    void calculateDuration_NoStartTime() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStartedAt(null); // Explicitly set to null to override constructor default
        
        Long duration = processService.calculateDuration(entity);
        
        assertNull(duration);
    }
    
    @Test
    void calculateDuration_NullEntity() {
        Long duration = processService.calculateDuration(null);
        
        assertNull(duration);
    }
    
    @Test
    void isProcessOverdue_OverdueProcess() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStatus(ProcessStatus.ACTIVE);
        entity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        boolean isOverdue = processService.isProcessOverdue(entity);
        
        assertTrue(isOverdue);
    }
    
    @Test
    void isProcessOverdue_OnTrackProcess() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStatus(ProcessStatus.ACTIVE);
        entity.setDeadline(Instant.now().plusSeconds(3600)); // 1 hour from now
        
        boolean isOverdue = processService.isProcessOverdue(entity);
        
        assertFalse(isOverdue);
    }
    
    @Test
    void isProcessOverdue_CompletedProcess() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStatus(ProcessStatus.COMPLETED);
        entity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        boolean isOverdue = processService.isProcessOverdue(entity);
        
        assertFalse(isOverdue); // Completed processes are never overdue
    }
    
    @Test
    void isProcessOverdue_NoDeadline() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entity.setStatus(ProcessStatus.ACTIVE);
        // No deadline set
        
        boolean isOverdue = processService.isProcessOverdue(entity);
        
        assertFalse(isOverdue);
    }
    
    @Test
    void isProcessOverdue_NullEntity() {
        boolean isOverdue = processService.isProcessOverdue(null);
        
        assertFalse(isOverdue);
    }
}