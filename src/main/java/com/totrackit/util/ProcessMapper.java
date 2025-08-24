package com.totrackit.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Utility class for mapping between ProcessEntity and DTOs.
 * Note: This mapper handles computed properties directly to avoid circular dependency with ProcessService.
 */
@Singleton
@Introspected
public class ProcessMapper {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProcessMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
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
        
        // Parse JSON fields
        response.setTags(parseTagsFromJson(entity.getTags()));
        response.setContext(parseContextFromJson(entity.getContext()));
        
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
    
    /**
     * Converts tags JSON string to List of ProcessTag objects.
     */
    private List<ProcessTag> parseTagsFromJson(String tagsJson) {
        if (tagsJson == null || tagsJson.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<ProcessTag>>() {});
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse tags JSON: {}", tagsJson, e);
            return List.of();
        }
    }
    
    /**
     * Converts context JSON string to Map.
     */
    private Map<String, Object> parseContextFromJson(String contextJson) {
        if (contextJson == null || contextJson.trim().isEmpty()) {
            return Map.of();
        }
        
        try {
            return objectMapper.readValue(contextJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse context JSON: {}", contextJson, e);
            return Map.of();
        }
    }
}