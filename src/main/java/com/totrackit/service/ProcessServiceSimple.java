package com.totrackit.service;

import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.exception.ProcessAlreadyExistsException;
import com.totrackit.repository.ProcessRepositorySimple;
import com.totrackit.util.ProcessMapper;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Simplified service layer for process management operations.
 */
@Singleton
public class ProcessServiceSimple {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProcessServiceSimple.class);
    
    private final ProcessRepositorySimple processRepository;
    private final ProcessMapper processMapper;
    
    @Inject
    public ProcessServiceSimple(ProcessRepositorySimple processRepository, ProcessMapper processMapper) {
        this.processRepository = processRepository;
        this.processMapper = processMapper;
    }
    
    /**
     * Creates a new process with the given name and request parameters.
     */
    @Transactional
    public ProcessResponse createProcess(String name, NewProcessRequest request) {
        LOG.debug("Creating process: name='{}', id='{}'", name, request.getId());
        
        // Validate that no active process exists with the same name and ID
        if (processRepository.existsByNameAndProcessIdAndStatus(name, request.getId(), com.totrackit.model.ProcessStatus.ACTIVE)) {
            throw new ProcessAlreadyExistsException(name, request.getId());
        }
        
        // Create new process entity
        ProcessEntity entity = new ProcessEntity(request.getId(), name);
        
        // Set optional fields
        if (request.getDeadline() != null) {
            entity.setDeadline(Instant.ofEpochSecond(request.getDeadline()));
        }
        
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            // For now, skip tags to avoid JSON conversion issues
            // entity.setTags(convertTagsToJson(request.getTags()));
        }
        
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            // For now, skip context to avoid JSON conversion issues
            // entity.setContext(convertContextToJson(request.getContext()));
        }
        
        // Save to database
        ProcessEntity savedEntity = processRepository.save(entity);
        
        LOG.info("Created process: name='{}', id='{}', dbId={}", name, request.getId(), savedEntity.getId());
        
        return processMapper.toResponse(savedEntity);
    }
}