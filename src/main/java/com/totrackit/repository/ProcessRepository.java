package com.totrackit.repository;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProcessEntity operations.
 * Provides CRUD operations and custom queries for process management.
 */
@Repository
public interface ProcessRepository extends CrudRepository<ProcessEntity, Long> {
    
    /**
     * Finds a process by name and process ID.
     * 
     * @param name the process name
     * @param processId the process ID
     * @return Optional containing the process if found
     */
    @Query("SELECT * FROM processes WHERE name = :name AND process_id = :processId")
    Optional<ProcessEntity> findByNameAndProcessId(String name, String processId);
    
    /**
     * Checks if an active process exists with the given name and process ID.
     * 
     * @param name the process name
     * @param processId the process ID
     * @return true if an active process exists
     */
    @Query("SELECT COUNT(*) > 0 FROM processes WHERE name = :name AND process_id = :processId AND status = 'ACTIVE'")
    boolean existsActiveProcess(String name, String processId);
    
    /**
     * Finds processes with optional filtering by status.
     * Results are ordered by started_at descending (newest first).
     * Note: Deadline status filtering is handled in the service layer for database compatibility.
     * 
     * @param status optional process status filter
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @return list of matching processes
     */
    @Query("SELECT * FROM processes WHERE " +
           "(:status IS NULL OR status = :status) " +
           "ORDER BY started_at DESC LIMIT :limit OFFSET :offset")
    List<ProcessEntity> findWithFilters(ProcessStatus status, int limit, int offset);
    
    /**
     * Finds processes by name with optional status filtering.
     * 
     * @param name the process name
     * @param status optional process status filter
     * @return list of matching processes
     */
    @Query("SELECT * FROM processes WHERE name = :name AND " +
           "(:status IS NULL OR status = :status) " +
           "ORDER BY started_at DESC")
    List<ProcessEntity> findByNameAndStatus(String name, ProcessStatus status);
    
    /**
     * Finds processes that contain specific tag key-value pairs.
     * Uses simple string containment for database compatibility.
     * 
     * @param tagKey the tag key to search for
     * @param tagValue the tag value to search for
     * @return list of processes containing the specified tag
     */
    @Query("SELECT * FROM processes WHERE " +
           "tags LIKE CONCAT('%\"key\":\"', :tagKey, '\"%') AND " +
           "tags LIKE CONCAT('%\"value\":\"', :tagValue, '\"%') " +
           "ORDER BY started_at DESC")
    List<ProcessEntity> findByTag(String tagKey, String tagValue);
    
    /**
     * Finds processes with deadline before a specific time.
     * 
     * @param deadline the deadline threshold
     * @return list of processes with deadline before the specified time
     */
    @Query("SELECT * FROM processes WHERE deadline < :deadline ORDER BY deadline ASC")
    List<ProcessEntity> findByDeadlineBefore(Instant deadline);
    
    /**
     * Finds processes with deadline after a specific time.
     * 
     * @param deadline the deadline threshold
     * @return list of processes with deadline after the specified time
     */
    @Query("SELECT * FROM processes WHERE deadline > :deadline ORDER BY deadline ASC")
    List<ProcessEntity> findByDeadlineAfter(Instant deadline);
    
    /**
     * Finds overdue processes (active processes past their deadline).
     * 
     * @param currentTime the current timestamp to compare against
     * @return list of overdue processes
     */
    @Query("SELECT * FROM processes WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline < :currentTime " +
           "ORDER BY deadline ASC")
    List<ProcessEntity> findOverdueProcesses(Instant currentTime);
    
    /**
     * Finds processes by status.
     * 
     * @param status the process status
     * @return list of processes with the specified status
     */
    List<ProcessEntity> findByStatus(ProcessStatus status);
    
    /**
     * Finds processes by name.
     * 
     * @param name the process name
     * @return list of processes with the specified name
     */
    List<ProcessEntity> findByName(String name);
    
    /**
     * Counts processes by status.
     * 
     * @param status the process status
     * @return count of processes with the specified status
     */
    long countByStatus(ProcessStatus status);
    
    /**
     * Counts total processes.
     * 
     * @return total count of processes
     */
    @Query("SELECT COUNT(*) FROM processes")
    long countAll();
    
    /**
     * Finds completed processes for metrics calculation.
     * 
     * @return list of completed processes ordered by completion time
     */
    @Query("SELECT * FROM processes WHERE status = 'COMPLETED' AND completed_at IS NOT NULL " +
           "ORDER BY completed_at DESC")
    List<ProcessEntity> findCompletedProcesses();
    
    /**
     * Finds processes with complex filtering including tag-based searches.
     * This method supports simple tag filtering using string containment.
     * 
     * @param status optional process status filter
     * @param tagFilter simple tag filter string
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @return list of matching processes
     */
    @Query("SELECT * FROM processes WHERE " +
           "(:status IS NULL OR status = :status) AND " +
           "(:tagFilter IS NULL OR tags LIKE CONCAT('%', :tagFilter, '%')) " +
           "ORDER BY started_at DESC LIMIT :limit OFFSET :offset")
    List<ProcessEntity> findWithTagFilters(ProcessStatus status, String tagFilter, 
                                          int limit, int offset);
}