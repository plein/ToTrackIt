package com.totrackit.service;

import com.totrackit.dto.*;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.exception.ProcessAlreadyCompletedException;
import com.totrackit.exception.ProcessAlreadyExistsException;
import com.totrackit.exception.ProcessNotFoundException;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
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
     * Validates that no active process exists with the same name and ID.
     * 
     * @param name the process name
     * @param request the process creation request
     * @return the created process response
     * @throws ProcessAlreadyExistsException if an active process already exists
     */
    @Transactional
    public ProcessResponse createProcess(String name, NewProcessRequest request) {
        LOG.debug("Creating process: name='{}', id='{}'", name, request.getId());
        
        // Validate that no active process exists with the same name and ID
        if (processRepository.existsActiveProcess(name, request.getId())) {
            throw new ProcessAlreadyExistsException(name, request.getId());
        }
        
        // Create new process entity
        ProcessEntity entity = new ProcessEntity(request.getId(), name);
        
        // Set optional fields
        if (request.getDeadline() != null) {
            entity.setDeadline(Instant.ofEpochSecond(request.getDeadline()));
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            entity.setTagsList(request.getTags());
        }
        
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            entity.setContextMap(request.getContext());
        }
        
        // Save to database
        ProcessEntity savedEntity = processRepository.save(entity);
        
        LOG.info("Created process: name='{}', id='{}', dbId={}", name, request.getId(), savedEntity.getId());
        
        return processMapper.toResponse(savedEntity);
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
                name, processId, status, savedEntity.getDuration());
        
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
        
        List<ProcessEntity> entities;
        
        // Apply filtering logic
        if (filter.getDeadlineStatus() == DeadlineStatus.MISSED) {
            // Special case for overdue processes
            entities = processRepository.findOverdueProcesses(Instant.now());
            entities = applyPagination(entities, pageable);
        } else if (filter.getTags() != null && !filter.getTags().isEmpty()) {
            // Tag-based filtering (simplified for database compatibility)
            entities = applyTagFiltering(filter, pageable);
        } else {
            // Standard filtering
            entities = processRepository.findWithFilters(
                    filter.getStatus(),
                    pageable.getLimit(),
                    pageable.getOffset()
            );
        }
        
        // Apply additional filtering in memory for complex criteria
        entities = applyAdditionalFiltering(entities, filter);
        
        // Convert to responses
        List<ProcessResponse> responses = entities.stream()
                .map(processMapper::toResponse)
                .collect(Collectors.toList());
        
        // Calculate total count (simplified approach)
        long total = calculateTotalCount(filter);
        
        LOG.debug("Found {} processes (total: {})", responses.size(), total);
        
        return new PagedResult<>(responses, total, pageable.getLimit(), pageable.getOffset());
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
        
        DeadlineStatus entityStatus = entity.getDeadlineStatus();
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
        
        Long duration = entity.getDuration();
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
     * Validates process creation request parameters.
     */
    private void validateCreateRequest(String name, NewProcessRequest request) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Process name cannot be null or empty");
        }
        
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Process ID cannot be null or empty");
        }
        
        if (request.getDeadline() != null && request.getDeadline() <= 0) {
            throw new IllegalArgumentException("Deadline must be a positive timestamp");
        }
    }
    
    /**
     * Validates process completion parameters.
     */
    private void validateCompleteRequest(String name, String processId, ProcessStatus status) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Process name cannot be null or empty");
        }
        
        if (processId == null || processId.trim().isEmpty()) {
            throw new IllegalArgumentException("Process ID cannot be null or empty");
        }
        
        if (status == null) {
            throw new IllegalArgumentException("Completion status cannot be null");
        }
        
        if (status == ProcessStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot complete process with ACTIVE status");
        }
    }
}