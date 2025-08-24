package com.totrackit.model;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

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
    void testBasicFieldOperations() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        // Test deadline setting
        Instant deadline = Instant.now().plusSeconds(3600);
        entity.setDeadline(deadline);
        assertEquals(deadline, entity.getDeadline());
        
        // Test status change
        entity.setStatus(ProcessStatus.COMPLETED);
        assertEquals(ProcessStatus.COMPLETED, entity.getStatus());
        
        // Test completion time
        Instant completedAt = Instant.now();
        entity.setCompletedAt(completedAt);
        assertEquals(completedAt, entity.getCompletedAt());
    }

    @Test
    void testJsonFieldOperations() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        // Test tags as JSON string
        String tagsJson = "[{\"key\":\"env\",\"value\":\"prod\"}]";
        entity.setTags(tagsJson);
        assertEquals(tagsJson, entity.getTags());
        
        // Test context as JSON string
        String contextJson = "{\"user_id\":\"123\",\"batch_size\":100}";
        entity.setContext(contextJson);
        assertEquals(contextJson, entity.getContext());
        
        // Test null values
        entity.setTags(null);
        assertNull(entity.getTags());
        
        entity.setContext(null);
        assertNull(entity.getContext());
    }

    @Test
    void testTimestampUpdates() {
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        Instant originalUpdatedAt = entity.getUpdatedAt();
        
        // Status change should update timestamp
        try {
            Thread.sleep(1); // Ensure time difference
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        entity.setStatus(ProcessStatus.COMPLETED);
        assertTrue(entity.getUpdatedAt().isAfter(originalUpdatedAt));
        
        // Completion should update timestamp
        Instant beforeCompletion = entity.getUpdatedAt();
        try {
            Thread.sleep(1); // Ensure time difference
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        entity.setCompletedAt(Instant.now());
        assertTrue(entity.getUpdatedAt().isAfter(beforeCompletion));
    }

    @Test
    void testEqualsAndHashCode() {
        ProcessEntity entity1 = new ProcessEntity("test-id", "test-process");
        ProcessEntity entity2 = new ProcessEntity("test-id", "test-process");
        
        // Entities with same ID should be equal
        entity1.setId(1L);
        entity2.setId(1L);
        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
        
        // Entities with different IDs should not be equal
        entity2.setId(2L);
        assertNotEquals(entity1, entity2);
    }
}