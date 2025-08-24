package com.totrackit.service;

import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.ProcessFilter;
import com.totrackit.dto.Pageable;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple validation tests for ProcessService business logic.
 * Tests validation methods and data structures without database dependencies.
 */
class ProcessServiceValidationTest {
    
    @Test
    void newProcessRequest_Validation() {
        // Test valid request
        NewProcessRequest validRequest = new NewProcessRequest("valid-id");
        validRequest.setDeadline(Instant.now().plusSeconds(3600).getEpochSecond());
        validRequest.setTags(List.of(new ProcessTag("env", "test")));
        validRequest.setContext(Map.of("key", "value"));
        
        assertNotNull(validRequest.getId());
        assertEquals("valid-id", validRequest.getId());
        assertNotNull(validRequest.getDeadline());
        assertEquals(1, validRequest.getTags().size());
        assertEquals("test", validRequest.getTags().get(0).getValue());
        assertEquals("value", validRequest.getContext().get("key"));
    }
    
    @Test
    void processFilter_DefaultValues() {
        ProcessFilter filter = new ProcessFilter();
        
        assertNull(filter.getStatus());
        assertNull(filter.getDeadlineStatus());
        assertNull(filter.getTags());
        assertNull(filter.getDeadlineBefore());
        assertNull(filter.getDeadlineAfter());
        assertNull(filter.getRunningDurationMin());
        assertEquals("started_at", filter.getSortBy());
        assertEquals("desc", filter.getSortDirection());
    }
    
    @Test
    void processFilter_WithValues() {
        ProcessFilter filter = new ProcessFilter();
        filter.setStatus(ProcessStatus.ACTIVE);
        filter.setDeadlineStatus(DeadlineStatus.ON_TRACK);
        filter.setTags(Map.of("env", "prod"));
        filter.setDeadlineBefore(Instant.now().getEpochSecond());
        filter.setDeadlineAfter(Instant.now().minusSeconds(3600).getEpochSecond());
        filter.setRunningDurationMin(300);
        filter.setSortBy("deadline");
        filter.setSortDirection("asc");
        
        assertEquals(ProcessStatus.ACTIVE, filter.getStatus());
        assertEquals(DeadlineStatus.ON_TRACK, filter.getDeadlineStatus());
        assertEquals("prod", filter.getTags().get("env"));
        assertNotNull(filter.getDeadlineBefore());
        assertNotNull(filter.getDeadlineAfter());
        assertEquals(300, filter.getRunningDurationMin());
        assertEquals("deadline", filter.getSortBy());
        assertEquals("asc", filter.getSortDirection());
    }
    
    @Test
    void pageable_DefaultValues() {
        Pageable pageable = new Pageable();
        
        assertEquals(20, pageable.getLimit());
        assertEquals(0, pageable.getOffset());
    }
    
    @Test
    void pageable_WithValues() {
        Pageable pageable = new Pageable(50, 100);
        
        assertEquals(50, pageable.getLimit());
        assertEquals(100, pageable.getOffset());
    }
    
    @Test
    void pageable_LimitConstraints() {
        // Test limit constraints (should be between 1 and 100)
        Pageable tooSmall = new Pageable(0, 0);
        assertEquals(1, tooSmall.getLimit()); // Should be clamped to 1
        
        Pageable tooLarge = new Pageable(200, 0);
        assertEquals(100, tooLarge.getLimit()); // Should be clamped to 100
        
        Pageable negativeOffset = new Pageable(10, -5);
        assertEquals(0, negativeOffset.getOffset()); // Should be clamped to 0
    }
    
    @Test
    void processTag_Creation() {
        ProcessTag tag = new ProcessTag("environment", "production");
        
        assertEquals("environment", tag.getKey());
        assertEquals("production", tag.getValue());
    }
    
    @Test
    void processTag_Equality() {
        ProcessTag tag1 = new ProcessTag("env", "test");
        ProcessTag tag2 = new ProcessTag("env", "test");
        ProcessTag tag3 = new ProcessTag("env", "prod");
        
        assertEquals(tag1, tag2);
        assertNotEquals(tag1, tag3);
        assertEquals(tag1.hashCode(), tag2.hashCode());
    }
    
    @Test
    void processStatus_Values() {
        assertEquals(3, ProcessStatus.values().length);
        assertTrue(List.of(ProcessStatus.values()).contains(ProcessStatus.ACTIVE));
        assertTrue(List.of(ProcessStatus.values()).contains(ProcessStatus.COMPLETED));
        assertTrue(List.of(ProcessStatus.values()).contains(ProcessStatus.FAILED));
    }
    
    @Test
    void deadlineStatus_Values() {
        assertEquals(4, DeadlineStatus.values().length);
        assertTrue(List.of(DeadlineStatus.values()).contains(DeadlineStatus.ON_TRACK));
        assertTrue(List.of(DeadlineStatus.values()).contains(DeadlineStatus.MISSED));
        assertTrue(List.of(DeadlineStatus.values()).contains(DeadlineStatus.COMPLETED_ON_TIME));
        assertTrue(List.of(DeadlineStatus.values()).contains(DeadlineStatus.COMPLETED_LATE));
    }
}