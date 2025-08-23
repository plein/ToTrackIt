package com.totrackit.util;

import com.totrackit.dto.ProcessResponse;
import com.totrackit.entity.ProcessEntity;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Singleton;

/**
 * Utility class for mapping between ProcessEntity and DTOs.
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
        response.setDeadlineStatus(entity.getDeadlineStatus());
        
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
        
        response.setDuration(entity.getDuration());
        response.setTags(entity.getTagsList());
        response.setContext(entity.getContextMap());
        
        return response;
    }
}