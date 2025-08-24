package com.totrackit.util;

import com.totrackit.dto.ProcessResponse;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Singleton;

import java.time.Instant;

/**
 * Utility class for mapping between ProcessEntity and DTOs.
 * Note: This mapper handles computed properties directly to avoid circular dependency with ProcessService.
 */
@Singleton
@Introspected
public class ProcessMapper {
    
    /**
     * Converts a ProcessEntity to a ProcessResponse DTO.
     */
    public ProcessResponse toResponse(ProcessEntity entity) {
        if (entity == null) {
            return null;
        }
        
        ProcessResponse response = new ProcessResponse();
        response.setId(entity.getProcessId());
        response.setName(entity.getName());
        response.setStatus(entity.getStatus());
        
        // Calculate deadline status
        response.setDeadlineStatus(calculateDeadlineStatus(entity));
        
        // Convert timestamps to Unix seconds
        if (entity.getStartedAt() != null) {
            response.setStartedAt(entity.getStartedAt().getEpochSecond());
        }
        
        if (entity.getCompletedAt() != null) {
            response.setCompletedAt(entity.getCompletedAt().getEpochSecond());
        }
        
        if (entity.getDeadline() != null) {
            response.setDeadline(entity.getDeadline().getEpochSecond());
        }
        
        // Calculate duration
        response.setDuration(calculateDuration(entity));
        
        // Parse JSON fields - for now, return empty collections to avoid JSON parsing issues
        response.setTags(java.util.List.of());
        response.setContext(java.util.Map.of());
        
        return response;
    }
    
    /**
     * Calculates deadline status for an entity.
     */
    private DeadlineStatus calculateDeadlineStatus(ProcessEntity entity) {
        if (entity.getDeadline() == null) {
            return null;
        }
        
        Instant now = Instant.now();
        
        if (entity.getStatus() == ProcessStatus.ACTIVE) {
            return now.isAfter(entity.getDeadline()) ? DeadlineStatus.MISSED : DeadlineStatus.ON_TRACK;
        } else if (entity.getStatus() == ProcessStatus.COMPLETED && entity.getCompletedAt() != null) {
            return entity.getCompletedAt().isBefore(entity.getDeadline()) || entity.getCompletedAt().equals(entity.getDeadline())
                ? DeadlineStatus.COMPLETED_ON_TIME 
                : DeadlineStatus.COMPLETED_LATE;
        }
        
        return null;
    }
    
    /**
     * Calculates duration for an entity.
     */
    private Long calculateDuration(ProcessEntity entity) {
        if (entity.getStartedAt() == null) {
            return null;
        }
        
        Instant endTime = entity.getCompletedAt() != null ? entity.getCompletedAt() : Instant.now();
        return endTime.getEpochSecond() - entity.getStartedAt().getEpochSecond();
    }
}