package com.totrackit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.totrackit.dto.*;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.exception.ProcessAlreadyCompletedException;
import com.totrackit.exception.ProcessAlreadyExistsException;
import com.totrackit.exception.ProcessNotFoundException;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.util.ProcessMapper;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for process management operations.
 * Handles business logic, validation, and coordination between repository and controllers.
 */
@Singleton
public class ProcessService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProcessService.class);
    
    private final ProcessRepository processRepository;
    private final ProcessMapper processMapper;
    
    @Inject
    public ProcessService(ProcessRepository processRepository, ProcessMapper processMapper) {
        this.processRepository = processRepository;
        this.processMapper = processMapper;
    }
    
    /**
     * Creates a new process with the given name and request parameters.
     * Uses optimal pattern: single insert with database uniqueness enforcement.
     * 
     * @param name the process name
     * @param request the process creation request
     * @return the created process response
     * @throws ProcessAlreadyExistsException if an active process already exists
     */
    @Transactional
    public ProcessResponse createProcess(String name, NewProcessRequest request) {
        LOG.debug("Creating process: name='{}', id='{}'", name, request.getId());
        
        // Validate input parameters
        validateCreateRequest(name, request);
        
        // Create new process entity
        ProcessEntity entity = new ProcessEntity(request.getId(), name);
        
        // Set optional fields
        if (request.getDeadline() != null) {
            entity.setDeadline(Instant.ofEpochSecond(request.getDeadline()));
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            entity.setTags(convertTagsToJson(request.getTags()));
        }
        
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            entity.setContext(convertContextToJson(request.getContext()));
        }
        
        try {
            // Single insert - let database enforce uniqueness via partial unique index
            ProcessEntity savedEntity = processRepository.save(entity);
            
            LOG.info("Created process: name='{}', id='{}', dbId={}", name, request.getId(), savedEntity.getId());
            
            return processMapper.toResponse(savedEntity);
            
        } catch (Exception e) {
            // Check if this is a unique constraint violation on our partial index
            if (isUniqueConstraintViolation(e)) {
                LOG.debug("Active process already exists: name='{}', id='{}'", name, request.getId());
                throw new ProcessAlreadyExistsException(name, request.getId());
            }
            // Re-throw other exceptions
            throw e;
        }
    }
    
    /**
     * Retrieves a specific process by name and ID.
     * 
     * @param name the process name
     * @param processId the process ID
     * @return the process response
     * @throws ProcessNotFoundException if the process is not found
     */
    public ProcessResponse getProcess(String name, String processId) {
        LOG.debug("Retrieving process: name='{}', id='{}'", name, processId);
        
        // Validate input parameters
        validateGetRequest(name, processId);
        
        ProcessEntity entity = processRepository.findByNameAndProcessId(name, processId)
                .orElseThrow(() -> new ProcessNotFoundException(name, processId));
        
        return processMapper.toResponse(entity);
    }
    
    /**
     * Completes a process with the specified status.
     * Validates that the process exists and is not already completed.
     * 
     * @param name the process name
     * @param processId the process ID
     * @param status the completion status (COMPLETED or FAILED)
     * @return the updated process response
     * @throws ProcessNotFoundException if the process is not found
     * @throws ProcessAlreadyCompletedException if the process is already completed
     */
    @Transactional
    public ProcessResponse completeProcess(String name, String processId, ProcessStatus status) {
        LOG.debug("Completing process: name='{}', id='{}', status={}", name, processId, status);
        
        // Validate input parameters
        validateCompleteRequest(name, processId, status);
        
        ProcessEntity entity = processRepository.findByNameAndProcessId(name, processId)
                .orElseThrow(() -> new ProcessNotFoundException(name, processId));
        
        // Validate that process is not already completed
        if (entity.getStatus() != ProcessStatus.ACTIVE) {
            throw new ProcessAlreadyCompletedException(name, processId);
        }
        
        // Update process status and completion time
        entity.setStatus(status);
        entity.setCompletedAt(Instant.now());
        
        ProcessEntity savedEntity = processRepository.update(entity);
        
        LOG.info("Completed process: name='{}', id='{}', status={}, duration={}s", 
                name, processId, status, calculateDuration(savedEntity));
        
        return processMapper.toResponse(savedEntity);
    }
    
    /**
     * Lists processes with optional filtering and pagination.
     * Supports filtering by status, deadline status, tags, and other criteria.
     * 
     * @param filter the filter criteria
     * @param pageable the pagination parameters
     * @return paged result of process responses
     */
    public PagedResult<ProcessResponse> listProcesses(ProcessFilter filter, Pageable pageable) {
        LOG.debug("Listing processes with filter: {}, pageable: {}", filter, pageable);
        
        // Validate input parameters
        if (filter == null) {
            filter = new ProcessFilter();
        }
        if (pageable == null) {
            pageable = new Pageable();
        }
        
        try {
            // Get all matching records from database (without pagination for now)
            // We'll apply pagination after additional filtering and sorting
            List<ProcessEntity> entities = processRepository.findWithComprehensiveFilters(
                    filter.getName(),
                    filter.getId(),
                    filter.getStatus(),
                    Integer.MAX_VALUE, // Get all records
                    0 // No offset
            );
            
            LOG.debug("Retrieved {} entities from repository", entities.size());
            
            // Apply additional filtering that can't be done at database level
            entities = applyAdditionalFiltering(entities, filter);
            
            LOG.debug("After additional filtering: {} entities", entities.size());
            
            // Calculate total count before pagination
            long total = entities.size();
            
            // Apply sorting
            entities = applySorting(entities, filter);
            
            // Apply pagination after filtering and sorting
            entities = applyPagination(entities, pageable);
            
            // Convert to responses
            List<ProcessResponse> responses = entities.stream()
                    .map(processMapper::toResponse)
                    .collect(Collectors.toList());
            
            LOG.debug("Final result: {} responses out of {} total", responses.size(), total);
            
            PagedResult<ProcessResponse> result = new PagedResult<>(responses, total, pageable.getLimit(), pageable.getOffset());
            
            LOG.debug("Returning result: {}", result);
            
            return result;
            
        } catch (Exception e) {
            LOG.error("Error listing processes", e);
            throw e;
        }
    }
    
    /**
     * Applies tag-based filtering using repository methods.
     */
    private List<ProcessEntity> applyTagFiltering(ProcessFilter filter, Pageable pageable) {
        // For simplicity, we'll use the first tag for filtering
        Map.Entry<String, String> firstTag = filter.getTags().entrySet().iterator().next();
        List<ProcessEntity> entities = processRepository.findByTag(firstTag.getKey(), firstTag.getValue());
        
        // Apply status filtering if specified
        if (filter.getStatus() != null) {
            entities = entities.stream()
                    .filter(e -> e.getStatus() == filter.getStatus())
                    .collect(Collectors.toList());
        }
        
        return applyPagination(entities, pageable);
    }
    
    /**
     * Applies sorting to a list of entities based on filter criteria.
     */
    private List<ProcessEntity> applySorting(List<ProcessEntity> entities, ProcessFilter filter) {
        if (entities.isEmpty()) {
            return entities;
        }
        
        String sortBy = filter.getSortBy() != null ? filter.getSortBy() : "started_at";
        String sortDirection = filter.getSortDirection() != null ? filter.getSortDirection() : "desc";
        boolean ascending = "asc".equalsIgnoreCase(sortDirection);
        
        return entities.stream()
                .sorted((e1, e2) -> {
                    int comparison = 0;
                    
                    switch (sortBy.toLowerCase()) {
                        case "started_at":
                            comparison = e1.getStartedAt().compareTo(e2.getStartedAt());
                            break;
                        case "completed_at":
                            if (e1.getCompletedAt() == null && e2.getCompletedAt() == null) {
                                comparison = 0;
                            } else if (e1.getCompletedAt() == null) {
                                comparison = 1; // null values last
                            } else if (e2.getCompletedAt() == null) {
                                comparison = -1; // null values last
                            } else {
                                comparison = e1.getCompletedAt().compareTo(e2.getCompletedAt());
                            }
                            break;
                        case "deadline":
                            if (e1.getDeadline() == null && e2.getDeadline() == null) {
                                comparison = 0;
                            } else if (e1.getDeadline() == null) {
                                comparison = 1; // null values last
                            } else if (e2.getDeadline() == null) {
                                comparison = -1; // null values last
                            } else {
                                comparison = e1.getDeadline().compareTo(e2.getDeadline());
                            }
                            break;
                        case "name":
                            comparison = e1.getName().compareTo(e2.getName());
                            break;
                        case "status":
                            comparison = e1.getStatus().compareTo(e2.getStatus());
                            break;
                        case "duration":
                            Long d1 = calculateDuration(e1);
                            Long d2 = calculateDuration(e2);
                            if (d1 == null && d2 == null) {
                                comparison = 0;
                            } else if (d1 == null) {
                                comparison = 1;
                            } else if (d2 == null) {
                                comparison = -1;
                            } else {
                                comparison = d1.compareTo(d2);
                            }
                            break;
                        default:
                            // Default to started_at for unknown sort fields
                            comparison = e1.getStartedAt().compareTo(e2.getStartedAt());
                    }
                    
                    return ascending ? comparison : -comparison;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Applies pagination to a list of entities.
     */
    private List<ProcessEntity> applyPagination(List<ProcessEntity> entities, Pageable pageable) {
        int start = Math.min(pageable.getOffset(), entities.size());
        int end = Math.min(start + pageable.getLimit(), entities.size());
        return entities.subList(start, end);
    }
    
    /**
     * Applies additional filtering criteria that cannot be handled at the database level.
     */
    private List<ProcessEntity> applyAdditionalFiltering(List<ProcessEntity> entities, ProcessFilter filter) {
        return entities.stream()
                .filter(entity -> matchesDeadlineStatus(entity, filter.getDeadlineStatus()))
                .filter(entity -> matchesDeadlineRange(entity, filter.getDeadlineBefore(), filter.getDeadlineAfter()))
                .filter(entity -> matchesRunningDuration(entity, filter.getRunningDurationMin()))
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if an entity matches the specified deadline status.
     */
    private boolean matchesDeadlineStatus(ProcessEntity entity, DeadlineStatus targetStatus) {
        if (targetStatus == null) {
            return true;
        }
        
        DeadlineStatus entityStatus = calculateDeadlineStatus(entity);
        return entityStatus == targetStatus;
    }
    
    /**
     * Checks if an entity matches the deadline range criteria.
     */
    private boolean matchesDeadlineRange(ProcessEntity entity, Long deadlineBefore, Long deadlineAfter) {
        if (entity.getDeadline() == null) {
            return deadlineBefore == null && deadlineAfter == null;
        }
        
        long entityDeadline = entity.getDeadline().getEpochSecond();
        
        if (deadlineBefore != null && entityDeadline >= deadlineBefore) {
            return false;
        }
        
        if (deadlineAfter != null && entityDeadline <= deadlineAfter) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if an entity matches the minimum running duration criteria.
     */
    private boolean matchesRunningDuration(ProcessEntity entity, Integer runningDurationMin) {
        if (runningDurationMin == null || entity.getStatus() != ProcessStatus.ACTIVE) {
            return true;
        }
        
        Long duration = calculateDuration(entity);
        return duration != null && duration >= runningDurationMin;
    }
    
    /**
     * Calculates the total count of processes matching the filter criteria.
     * This is a simplified implementation for the current phase.
     */
    private long calculateTotalCount(ProcessFilter filter) {
        if (filter.getStatus() != null) {
            return processRepository.countByStatus(filter.getStatus());
        }
        return processRepository.countAll();
    }
    
    /**
     * Calculates deadline status for a process entity in real-time.
     * This method provides the business logic for deadline status calculation.
     * 
     * @param entity the process entity
     * @return the calculated deadline status
     */
    public DeadlineStatus calculateDeadlineStatus(ProcessEntity entity) {
        if (entity == null || entity.getDeadline() == null) {
            return null;
        }
        
        Instant now = Instant.now();
        Instant deadline = entity.getDeadline();
        
        if (entity.getStatus() == ProcessStatus.ACTIVE) {
            return now.isAfter(deadline) ? DeadlineStatus.MISSED : DeadlineStatus.ON_TRACK;
        } else if (entity.getStatus() == ProcessStatus.COMPLETED && entity.getCompletedAt() != null) {
            return entity.getCompletedAt().isBefore(deadline) || entity.getCompletedAt().equals(deadline)
                ? DeadlineStatus.COMPLETED_ON_TIME 
                : DeadlineStatus.COMPLETED_LATE;
        }
        
        return null; // For FAILED status or other edge cases
    }
    
    /**
     * Calculates the current duration of a process in seconds.
     * For active processes, calculates from start time to now.
     * For completed processes, calculates from start time to completion time.
     * 
     * @param entity the process entity
     * @return the duration in seconds, or null if start time is not available
     */
    public Long calculateDuration(ProcessEntity entity) {
        if (entity == null || entity.getStartedAt() == null) {
            return null;
        }
        
        Instant endTime = entity.getCompletedAt() != null ? entity.getCompletedAt() : Instant.now();
        return endTime.getEpochSecond() - entity.getStartedAt().getEpochSecond();
    }
    
    /**
     * Checks if a process is currently overdue.
     * A process is overdue if it's active and past its deadline.
     * 
     * @param entity the process entity
     * @return true if the process is overdue, false otherwise
     */
    public boolean isProcessOverdue(ProcessEntity entity) {
        return entity != null && 
               entity.getStatus() == ProcessStatus.ACTIVE && 
               entity.getDeadline() != null && 
               Instant.now().isAfter(entity.getDeadline());
    }
    
    /**
     * Converts tags JSON string to List of ProcessTag objects.
     */
    public List<ProcessTag> parseTagsFromJson(String tagsJson) {
        if (tagsJson == null || tagsJson.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(tagsJson, new TypeReference<List<ProcessTag>>() {});
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse tags JSON: {}", tagsJson, e);
            return List.of();
        }
    }
    
    /**
     * Converts List of ProcessTag objects to JSON string.
     */
    public String convertTagsToJson(List<ProcessTag> tagsList) {
        if (tagsList == null || tagsList.isEmpty()) {
            return null;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(tagsList);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to convert tags to JSON: {}", tagsList, e);
            return null;
        }
    }
    
    /**
     * Converts context JSON string to Map.
     */
    public Map<String, Object> parseContextFromJson(String contextJson) {
        if (contextJson == null || contextJson.trim().isEmpty()) {
            return Map.of();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(contextJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse context JSON: {}", contextJson, e);
            return Map.of();
        }
    }
    
    /**
     * Converts Map to context JSON string.
     */
    public String convertContextToJson(Map<String, Object> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            return null;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(contextMap);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to convert context to JSON: {}", contextMap, e);
            return null;
        }
    }
    
    /**
     * Validates process creation request parameters.
     */
    private void validateCreateRequest(String name, NewProcessRequest request) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Process name cannot be null or empty");
        }
        
        if (name.length() > 100) {
            throw new IllegalArgumentException("Process name cannot exceed 100 characters");
        }
        
        if (request == null) {
            throw new IllegalArgumentException("Process request cannot be null");
        }
        
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Process ID cannot be null or empty");
        }
        
        if (request.getId().length() > 50) {
            throw new IllegalArgumentException("Process ID cannot exceed 50 characters");
        }
        
        if (request.getDeadline() != null && request.getDeadline() <= 0) {
            throw new IllegalArgumentException("Deadline must be a positive timestamp");
        }
        
        // Validate deadline is not in the past (with some tolerance for clock skew)
        if (request.getDeadline() != null) {
            long currentTime = Instant.now().getEpochSecond();
            if (request.getDeadline() < currentTime - 60) { // Allow 1 minute tolerance
                throw new IllegalArgumentException("Deadline cannot be in the past");
            }
        }
        
        // Validate tags if present
        if (request.getTags() != null) {
            if (request.getTags().size() > 20) {
                throw new IllegalArgumentException("Cannot have more than 20 tags per process");
            }
            
            for (ProcessTag tag : request.getTags()) {
                if (tag.getKey() == null || tag.getKey().trim().isEmpty()) {
                    throw new IllegalArgumentException("Tag key cannot be null or empty");
                }
                if (tag.getValue() == null || tag.getValue().trim().isEmpty()) {
                    throw new IllegalArgumentException("Tag value cannot be null or empty");
                }
                if (tag.getKey().length() > 50) {
                    throw new IllegalArgumentException("Tag key cannot exceed 50 characters");
                }
                if (tag.getValue().length() > 100) {
                    throw new IllegalArgumentException("Tag value cannot exceed 100 characters");
                }
            }
        }
    }
    
    /**
     * Validates process completion parameters.
     */
    private void validateCompleteRequest(String name, String processId, ProcessStatus status) {
        if (status == ProcessStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot complete process with ACTIVE status");
        }
    }

    /**
     * Validates process retrieval parameters.
     */
    private void validateGetRequest(String name, String processId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Process name cannot be null or empty");
        }
        
        if (name.length() > 100) {
            throw new IllegalArgumentException("Process name cannot exceed 100 characters");
        }
        
        if (processId == null || processId.trim().isEmpty()) {
            throw new IllegalArgumentException("Process ID cannot be null or empty");
        }
        
        if (processId.length() > 50) {
            throw new IllegalArgumentException("Process ID cannot exceed 50 characters");
        }
    }
    
    /**
     * Checks if the given exception is a unique constraint violation.
     * Specifically detects violations of our partial unique index for active processes.
     * 
     * @param e the exception to check
     * @return true if this is a unique constraint violation, false otherwise
     */
    private boolean isUniqueConstraintViolation(Exception e) {
        // Walk the exception chain to find the root cause
        Throwable cause = e;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                // PostgreSQL unique constraint violation on our specific index
                if (message.contains("duplicate key value violates unique constraint") &&
                    message.contains("idx_processes_unique_active")) {
                    return true;
                }
                
                // Generic unique constraint patterns as fallback
                if (message.contains("duplicate key value violates unique constraint") &&
                    message.contains("processes")) {
                    return true;
                }
                
                // Generic data integrity violation patterns
                if (message.contains("DataIntegrityViolationException") &&
                    message.contains("duplicate key")) {
                    return true;
                }
                
                if (message.contains("ConstraintViolationException")) {
                    return true;
                }
            }
            
            // Check exception type
            String className = cause.getClass().getName();
            if (className.contains("DataIntegrityViolationException") ||
                className.contains("ConstraintViolationException")) {
                return true;
            }
            
            cause = cause.getCause();
        }
        
        return false;
    }
}