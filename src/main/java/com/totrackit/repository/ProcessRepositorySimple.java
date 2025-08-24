package com.totrackit.repository;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Simplified repository interface for ProcessEntity operations.
 */
@Repository
public interface ProcessRepositorySimple extends CrudRepository<ProcessEntity, Long> {
    
    /**
     * Finds a process by name and process ID using method naming convention.
     */
    Optional<ProcessEntity> findByNameAndProcessId(String name, String processId);
    
    /**
     * Checks if a process exists by name, process ID and status.
     */
    boolean existsByNameAndProcessIdAndStatus(String name, String processId, ProcessStatus status);
}