package com.totrackit.repository;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProcessEntity operations.
 * Provides CRUD operations and custom queries for process management.
 * List filtering/pagination lives in {@link ProcessQueryRepository}; every
 * query here is either point-lookup or bounded by an explicit batch limit.
 */
@JdbcRepository(dialect = Dialect.POSTGRES)
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
     * Finds overdue active processes that have not yet had a deadline-breach
     * notification sent, oldest deadline first, bounded per scan cycle.
     *
     * @param currentTime the current timestamp to compare against
     * @param batch maximum rows to process in one cycle
     * @return list of overdue, unnotified processes
     */
    @Query("SELECT * FROM processes WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline < :currentTime " +
           "AND deadline_notified_at IS NULL ORDER BY deadline ASC LIMIT :batch")
    List<ProcessEntity> findOverdueUnnotified(Instant currentTime, int batch);

    /**
     * Finds active processes that have crossed the pre-deadline warning
     * threshold (fraction of the started_at→deadline budget) and have not yet
     * been warned. The threshold math runs in SQL against the partial index on
     * unwarned future deadlines, so the scan never loads rows that are not
     * actionable this cycle.
     *
     * @param currentTime the current timestamp to compare against
     * @param threshold warning threshold as a fraction of the deadline budget (0..1)
     * @param batch maximum rows to process in one cycle
     * @return list of processes due a warning, soonest deadline first
     */
    @Query("SELECT * FROM processes WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline > :currentTime " +
           "AND deadline_warned_at IS NULL AND deadline > started_at " +
           "AND :currentTime >= started_at + (deadline - started_at) * :threshold " +
           "ORDER BY deadline ASC LIMIT :batch")
    List<ProcessEntity> findApproachingUnwarned(Instant currentTime, double threshold, int batch);

    /**
     * Counts the missed-deadline notification backlog (overdue, unnotified).
     *
     * @param currentTime the current timestamp to compare against
     * @return number of overdue processes still awaiting processing
     */
    @Query("SELECT COUNT(*) FROM processes WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline < :currentTime " +
           "AND deadline_notified_at IS NULL")
    long countOverdueUnnotified(Instant currentTime);

    /**
     * Counts the warning backlog (past the warning threshold, unwarned).
     *
     * @param currentTime the current timestamp to compare against
     * @param threshold warning threshold as a fraction of the deadline budget (0..1)
     * @return number of at-risk processes still awaiting processing
     */
    @Query("SELECT COUNT(*) FROM processes WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline > :currentTime " +
           "AND deadline_warned_at IS NULL AND deadline > started_at " +
           "AND :currentTime >= started_at + (deadline - started_at) * :threshold")
    long countApproachingUnwarned(Instant currentTime, double threshold);

    /**
     * Marks a process as having been notified about its missed deadline.
     *
     * @param id the internal process ID
     * @param notifiedAt when the notification was sent
     */
    @Query("UPDATE processes SET deadline_notified_at = :notifiedAt WHERE id = :id")
    void markDeadlineNotified(Long id, Instant notifiedAt);

    /**
     * Marks a batch of processes as notified in one round trip.
     *
     * @param ids the internal process IDs
     * @param notifiedAt when the notifications were sent
     */
    @Query("UPDATE processes SET deadline_notified_at = :notifiedAt WHERE id IN (:ids)")
    void markDeadlineNotifiedBatch(List<Long> ids, Instant notifiedAt);

    /**
     * Marks a process as having had its pre-deadline warning processed.
     *
     * @param id the internal process ID
     * @param warnedAt when the warning was processed
     */
    @Query("UPDATE processes SET deadline_warned_at = :warnedAt WHERE id = :id")
    void markDeadlineWarned(Long id, Instant warnedAt);

    /**
     * Marks a batch of processes as warned in one round trip.
     *
     * @param ids the internal process IDs
     * @param warnedAt when the warnings were processed
     */
    @Query("UPDATE processes SET deadline_warned_at = :warnedAt WHERE id IN (:ids)")
    void markDeadlineWarnedBatch(List<Long> ids, Instant warnedAt);

    /**
     * Counts processes by status.
     *
     * @param status the process status
     * @return count of processes with the specified status
     */
    @Query("SELECT COUNT(*) FROM processes WHERE (:status IS NULL OR status = :status)")
    long countByStatus(@Nullable ProcessStatus status);
}
