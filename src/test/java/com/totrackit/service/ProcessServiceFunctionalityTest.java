package com.totrackit.service;

import com.totrackit.dto.*;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import com.totrackit.util.ProcessMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify ProcessService functionality and business logic.
 * These tests verify the service methods work correctly with the entity model.
 */
class ProcessServiceFunctionalityTest {
    
    @Test
    void processEntity_DeadlineStatusCalculation() {
        // Test ON_TRACK status
        ProcessEntity onTrackEntity = new ProcessEntity("test-id", "test-process");
        onTrackEntity.setDeadline(Instant.now().plusSeconds(3600)); // 1 hour from now
        onTrackEntity.setStatus(ProcessStatus.ACTIVE);
        
        assertEquals(DeadlineStatus.ON_TRACK, onTrackEntity.getDeadlineStatus());
        
        // Test MISSED status
        ProcessEntity missedEntity = new ProcessEntity("missed-id", "missed-process");
        missedEntity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        missedEntity.setStatus(ProcessStatus.ACTIVE);
        
        assertEquals(DeadlineStatus.MISSED, missedEntity.getDeadlineStatus());
        
        // Test COMPLETED_ON_TIME status
        ProcessEntity completedOnTimeEntity = new ProcessEntity("completed-id", "completed-process");
        Instant deadline = Instant.now().plusSeconds(3600);
        completedOnTimeEntity.setDeadline(deadline);
        completedOnTimeEntity.setStatus(ProcessStatus.COMPLETED);
        completedOnTimeEntity.setCompletedAt(deadline.minusSeconds(300)); // Completed 5 minutes early
        
        assertEquals(DeadlineStatus.COMPLETED_ON_TIME, completedOnTimeEntity.getDeadlineStatus());
        
        // Test COMPLETED_LATE status
        ProcessEntity completedLateEntity = new ProcessEntity("late-id", "late-process");
        Instant lateDeadline = Instant.now().minusSeconds(3600);
        completedLateEntity.setDeadline(lateDeadline);
        completedLateEntity.setStatus(ProcessStatus.COMPLETED);
        completedLateEntity.setCompletedAt(lateDeadline.plusSeconds(300)); // Completed 5 minutes late
        
        assertEquals(DeadlineStatus.COMPLETED_LATE, completedLateEntity.getDeadlineStatus());
    }
    
    @Test
    void processEntity_DurationCalculation() {
        ProcessEntity entity = new ProcessEntity("duration-test", "duration-process");
        Instant startTime = Instant.now().minusSeconds(300); // Started 5 minutes ago
        entity.setStartedAt(startTime);
        
        // For active process, duration should be current time - start time
        Long duration = entity.getDuration();
        assertNotNull(duration);
        assertTrue(duration >= 300); // At least 5 minutes
        assertTrue(duration <= 310); // Allow some tolerance for test execution time
        
        // For completed process, duration should be completion time - start time
        Instant completionTime = startTime.plusSeconds(600); // Completed after 10 minutes
        entity.setCompletedAt(completionTime);
        entity.setStatus(ProcessStatus.COMPLETED);
        
        assertEquals(600L, entity.getDuration());
    }
    
    @Test
    void processEntity_OverdueDetection() {
        // Test overdue process
        ProcessEntity overdueEntity = new ProcessEntity("overdue-id", "overdue-process");
        overdueEntity.setStatus(ProcessStatus.ACTIVE);
        overdueEntity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        assertTrue(overdueEntity.isOverdue());
        
        // Test on-track process
        ProcessEntity onTrackEntity = new ProcessEntity("ontrack-id", "ontrack-process");
        onTrackEntity.setStatus(ProcessStatus.ACTIVE);
        onTrackEntity.setDeadline(Instant.now().plusSeconds(3600)); // 1 hour from now
        
        assertFalse(onTrackEntity.isOverdue());
        
        // Test completed process (should not be overdue regardless of deadline)
        ProcessEntity completedEntity = new ProcessEntity("completed-id", "completed-process");
        completedEntity.setStatus(ProcessStatus.COMPLETED);
        completedEntity.setDeadline(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        assertFalse(completedEntity.isOverdue());
        
        // Test process without deadline
        ProcessEntity noDeadlineEntity = new ProcessEntity("no-deadline-id", "no-deadline-process");
        noDeadlineEntity.setStatus(ProcessStatus.ACTIVE);
        
        assertFalse(noDeadlineEntity.isOverdue());
    }
    
    @Test
    void processEntity_TagsHandling() {
        ProcessEntity entity = new ProcessEntity("tags-test", "tags-process");
        
        // Test setting and getting tags
        List<ProcessTag> tags = List.of(
            new ProcessTag("env", "production"),
            new ProcessTag("team", "backend"),
            new ProcessTag("priority", "high")
        );
        
        entity.setTagsList(tags);
        
        List<ProcessTag> retrievedTags = entity.getTagsList();
        assertEquals(3, retrievedTags.size());
        assertTrue(retrievedTags.contains(new ProcessTag("env", "production")));
        assertTrue(retrievedTags.contains(new ProcessTag("team", "backend")));
        assertTrue(retrievedTags.contains(new ProcessTag("priority", "high")));
        
        // Test empty tags
        entity.setTagsList(List.of());
        assertTrue(entity.getTagsList().isEmpty());
        
        // Test null tags
        entity.setTagsList(null);
        assertTrue(entity.getTagsList().isEmpty());
    }
    
    @Test
    void processEntity_ContextHandling() {
        ProcessEntity entity = new ProcessEntity("context-test", "context-process");
        
        // Test setting and getting context
        Map<String, Object> context = Map.of(
            "userId", "user123",
            "requestId", "req456",
            "metadata", Map.of("source", "api", "version", "1.0")
        );
        
        entity.setContextMap(context);
        
        Map<String, Object> retrievedContext = entity.getContextMap();
        assertEquals(3, retrievedContext.size());
        assertEquals("user123", retrievedContext.get("userId"));
        assertEquals("req456", retrievedContext.get("requestId"));
        assertTrue(retrievedContext.get("metadata") instanceof Map);
        
        // Test empty context
        entity.setContextMap(Map.of());
        assertTrue(entity.getContextMap().isEmpty());
        
        // Test null context
        entity.setContextMap(null);
        assertTrue(entity.getContextMap().isEmpty());
    }
    
    @Test
    void processMapper_EntityToResponse() {
        ProcessMapper mapper = new ProcessMapper();
        
        // Create a complete entity
        ProcessEntity entity = new ProcessEntity("mapper-test", "mapper-process");
        entity.setId(123L);
        entity.setStatus(ProcessStatus.ACTIVE);
        entity.setStartedAt(Instant.now().minusSeconds(300));
        entity.setDeadline(Instant.now().plusSeconds(3600));
        entity.setTagsList(List.of(new ProcessTag("env", "test")));
        entity.setContextMap(Map.of("key", "value"));
        
        // Map to response
        ProcessResponse response = mapper.toResponse(entity);
        
        assertNotNull(response);
        assertEquals("mapper-test", response.getId());
        assertEquals("mapper-process", response.getName());
        assertEquals(ProcessStatus.ACTIVE, response.getStatus());
        assertEquals(DeadlineStatus.ON_TRACK, response.getDeadlineStatus());
        assertNotNull(response.getStartedAt());
        assertNull(response.getCompletedAt());
        assertNotNull(response.getDeadline());
        assertNotNull(response.getDuration());
        assertEquals(1, response.getTags().size());
        assertEquals("env", response.getTags().get(0).getKey());
        assertEquals("test", response.getTags().get(0).getValue());
        assertEquals("value", response.getContext().get("key"));
    }
    
    @Test
    void processMapper_NullEntity() {
        ProcessMapper mapper = new ProcessMapper();
        
        ProcessResponse response = mapper.toResponse(null);
        
        assertNull(response);
    }
    
    @Test
    void pagedResult_Construction() {
        List<ProcessResponse> data = List.of(
            new ProcessResponse("id1", "process1", ProcessStatus.ACTIVE),
            new ProcessResponse("id2", "process2", ProcessStatus.COMPLETED)
        );
        
        PagedResult<ProcessResponse> result = new PagedResult<>(data, 10L, 5, 0);
        
        assertEquals(2, result.getData().size());
        assertEquals(10L, result.getTotal());
        assertEquals(5, result.getLimit());
        assertEquals(0, result.getOffset());
        assertTrue(result.isHasMore()); // 2 items returned, but total is 10, so has more
        
        // Test case where there are no more items
        PagedResult<ProcessResponse> noMoreResult = new PagedResult<>(data, 2L, 5, 0);
        assertFalse(noMoreResult.isHasMore()); // 2 items returned, total is 2, so no more
    }
}