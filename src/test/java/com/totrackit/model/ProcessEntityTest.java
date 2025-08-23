package com.totrackit.model;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessEntityTest {

    @Test
    void testProcessEntityCreation() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        assertEquals("test-id", entity.getProcessId());
        assertEquals("test-process", entity.getName());
        assertEquals(ProcessStatus.ACTIVE, entity.getStatus());
        assertNotNull(entity.getStartedAt());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
    }

    @Test
    void testDeadlineStatusCalculation() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        // No deadline
        assertNull(entity.getDeadlineStatus());
        
        // Active process with future deadline
        entity.setDeadline(Instant.now().plusSeconds(3600)); // 1 hour from now
        assertEquals(DeadlineStatus.ON_TRACK, entity.getDeadlineStatus());
        
        // Active process with past deadline
        entity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        assertEquals(DeadlineStatus.MISSED, entity.getDeadlineStatus());
        
        // Completed process on time
        entity.setStatus(ProcessStatus.COMPLETED);
        entity.setCompletedAt(Instant.now().minusSeconds(1800)); // 30 minutes ago
        entity.setDeadline(Instant.now().minusSeconds(900)); // 15 minutes ago
        assertEquals(DeadlineStatus.COMPLETED_ON_TIME, entity.getDeadlineStatus());
        
        // Completed process late
        entity.setCompletedAt(Instant.now().minusSeconds(900)); // 15 minutes ago
        entity.setDeadline(Instant.now().minusSeconds(1800)); // 30 minutes ago
        assertEquals(DeadlineStatus.COMPLETED_LATE, entity.getDeadlineStatus());
    }

    @Test
    void testDurationCalculation() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        // Active process duration
        assertNotNull(entity.getDuration());
        assertTrue(entity.getDuration() >= 0);
        
        // Completed process duration
        Instant startTime = Instant.now().minusSeconds(3600); // 1 hour ago
        Instant endTime = Instant.now().minusSeconds(1800); // 30 minutes ago
        entity.setStartedAt(startTime);
        entity.setCompletedAt(endTime);
        
        assertEquals(1800L, entity.getDuration()); // 30 minutes = 1800 seconds
    }

    @Test
    void testOverdueCheck() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        // No deadline
        assertFalse(entity.isOverdue());
        
        // Future deadline
        entity.setDeadline(Instant.now().plusSeconds(3600));
        assertFalse(entity.isOverdue());
        
        // Past deadline, active process
        entity.setDeadline(Instant.now().minusSeconds(3600));
        assertTrue(entity.isOverdue());
        
        // Past deadline, completed process
        entity.setStatus(ProcessStatus.COMPLETED);
        assertFalse(entity.isOverdue());
    }

    @Test
    void testTagsSerialization() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        List<ProcessTag> tags = List.of(
            new ProcessTag("environment", "production"),
            new ProcessTag("team", "backend")
        );
        
        entity.setTagsList(tags);
        assertNotNull(entity.getTags());
        
        List<ProcessTag> retrievedTags = entity.getTagsList();
        assertEquals(2, retrievedTags.size());
        assertEquals("environment", retrievedTags.get(0).getKey());
        assertEquals("production", retrievedTags.get(0).getValue());
    }

    @Test
    void testContextSerialization() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        Map<String, Object> context = Map.of(
            "user_id", "12345",
            "batch_size", 100,
            "retry_count", 3
        );
        
        entity.setContextMap(context);
        assertNotNull(entity.getContext());
        
        Map<String, Object> retrievedContext = entity.getContextMap();
        assertEquals(3, retrievedContext.size());
        assertEquals("12345", retrievedContext.get("user_id"));
        assertEquals(100, retrievedContext.get("batch_size"));
    }
}