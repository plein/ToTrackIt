package com.totrackit.service;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for recording custom application metrics.
 * Handles process-related metrics and database operation metrics.
 */
@Singleton
public class MetricsService {
    
    private static final Logger LOG = LoggerFactory.getLogger(MetricsService.class);
    
    private final MeterRegistry meterRegistry;

    // Gauges must reference a live object; re-registering with a boxed number
    // binds the gauge to the first value forever. These holders are updated
    // in place and strongly referenced for the lifetime of the service.
    private final AtomicLong activeProcesses = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> overdueByName = new ConcurrentHashMap<>();

    @Inject
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder("totrackit_active_processes_current", activeProcesses, AtomicLong::get)
                .description("Current number of active processes")
                .register(meterRegistry);
    }
    
    /**
     * Records a process creation event.
     * 
     * @param processName the name of the process
     */
    public void recordProcessCreated(String processName) {
        try {
            Counter.builder("totrackit_processes_created_total")
                    .description("Total number of processes created")
                    .tag("process_name", processName != null ? processName : "unknown")
                    .register(meterRegistry)
                    .increment();
            LOG.debug("Recorded process created metric for: {}", processName);
        } catch (Exception e) {
            LOG.warn("Failed to record process created metric", e);
        }
    }
    
    /**
     * Records a process completion event.
     * 
     * @param entity the completed process entity
     */
    public void recordProcessCompleted(ProcessEntity entity) {
        if (entity == null) {
            return;
        }
        
        try {
            String processName = entity.getName() != null ? entity.getName() : "unknown";
            String status = entity.getStatus() != null ? entity.getStatus().toString() : "unknown";
            
            if (entity.getStatus() == ProcessStatus.COMPLETED) {
                Counter.builder("totrackit_processes_completed_total")
                        .description("Total number of processes completed successfully")
                        .tag("process_name", processName)
                        .tag("status", status)
                        .register(meterRegistry)
                        .increment();
            } else if (entity.getStatus() == ProcessStatus.FAILED) {
                Counter.builder("totrackit_processes_failed_total")
                        .description("Total number of processes that failed")
                        .tag("process_name", processName)
                        .tag("status", status)
                        .register(meterRegistry)
                        .increment();
            }
            
            // Record deadline outcome for completed processes that had a deadline.
            // These counters let external monitors (Prometheus alerts, Datadog SLOs)
            // compute on-time rates without querying the API.
            if (entity.getStatus() == ProcessStatus.COMPLETED
                    && entity.getDeadline() != null && entity.getCompletedAt() != null) {
                boolean onTime = !entity.getCompletedAt().isAfter(entity.getDeadline());
                Counter.builder(onTime
                                ? "totrackit_processes_completed_on_time_total"
                                : "totrackit_processes_completed_late_total")
                        .description(onTime
                                ? "Total number of processes completed before their deadline"
                                : "Total number of processes completed after their deadline")
                        .tag("process_name", processName)
                        .register(meterRegistry)
                        .increment();
            }

            // Record process duration if we have both start and completion times
            if (entity.getStartedAt() != null && entity.getCompletedAt() != null) {
                Duration duration = Duration.between(entity.getStartedAt(), entity.getCompletedAt());
                Timer.builder("totrackit_process_duration_seconds")
                        .description("Duration of completed processes in seconds")
                        .tag("process_name", processName)
                        .tag("status", status)
                        .register(meterRegistry)
                        .record(duration.toMillis(), TimeUnit.MILLISECONDS);
                
                LOG.debug("Recorded process completion metric for: {} (duration: {}ms)", 
                         processName, duration.toMillis());
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to record process completion metric", e);
        }
    }
    
    /**
     * Records a database operation event.
     * 
     * @param operation the type of database operation (create, read, update, delete)
     * @param table the table name
     * @param success whether the operation was successful
     */
    public void recordDatabaseOperation(String operation, String table, boolean success) {
        try {
            Counter.builder("totrackit_database_operations_total")
                    .description("Total number of database operations")
                    .tag("operation", operation != null ? operation : "unknown")
                    .tag("table", table != null ? table : "unknown")
                    .tag("success", String.valueOf(success))
                    .register(meterRegistry)
                    .increment();
                    
            LOG.debug("Recorded database operation metric: {} on {} (success: {})", 
                     operation, table, success);
        } catch (Exception e) {
            LOG.warn("Failed to record database operation metric", e);
        }
    }
    
    /**
     * Records HTTP request metrics with labels.
     * This method can be used to supplement the automatic HTTP metrics.
     * 
     * @param method the HTTP method
     * @param endpoint the endpoint path
     * @param statusCode the response status code
     * @param duration the request duration in milliseconds
     */
    public void recordHttpRequest(String method, String endpoint, int statusCode, long duration) {
        try {
            Timer.builder("totrackit_http_request_duration_seconds")
                    .description("HTTP request duration in seconds")
                    .tag("method", method != null ? method : "unknown")
                    .tag("endpoint", endpoint != null ? endpoint : "unknown")
                    .tag("status", String.valueOf(statusCode))
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);
                    
            Counter.builder("totrackit_http_requests_total")
                    .description("Total number of HTTP requests")
                    .tag("method", method != null ? method : "unknown")
                    .tag("endpoint", endpoint != null ? endpoint : "unknown")
                    .tag("status", String.valueOf(statusCode))
                    .register(meterRegistry)
                    .increment();
                    
            LOG.debug("Recorded HTTP request metric: {} {} -> {} ({}ms)", 
                     method, endpoint, statusCode, duration);
        } catch (Exception e) {
            LOG.warn("Failed to record HTTP request metric", e);
        }
    }
    
    /**
     * Records current active processes count as a gauge.
     * This should be called periodically to update the gauge value.
     *
     * @param count the current number of active processes
     */
    public void recordActiveProcessesCount(long count) {
        activeProcesses.set(count);
        LOG.debug("Updated active processes gauge: {}", count);
    }

    /**
     * Records that a process missed its deadline. Incremented once per process,
     * when the deadline scanner first processes the breach.
     *
     * @param processName the name of the process that missed its deadline
     */
    public void recordDeadlineMissed(String processName) {
        try {
            Counter.builder("totrackit_processes_deadline_missed_total")
                    .description("Total number of processes that missed their deadline")
                    .tag("process_name", processName != null ? processName : "unknown")
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            LOG.warn("Failed to record deadline missed metric", e);
        }
    }

    /**
     * Updates the per-process-name gauge of active processes currently past
     * their deadline. Names missing from the snapshot are reset to zero so a
     * recovered process name doesn't keep alerting.
     *
     * @param countsByName current overdue counts keyed by process name
     */
    public void updateOverdueProcessCounts(Map<String, Long> countsByName) {
        try {
            countsByName.forEach((name, count) -> overdueGaugeFor(name).set(count));
            overdueByName.forEach((name, holder) -> {
                if (!countsByName.containsKey(name)) {
                    holder.set(0);
                }
            });
        } catch (Exception e) {
            LOG.warn("Failed to update overdue process gauges", e);
        }
    }

    private AtomicLong overdueGaugeFor(String processName) {
        return overdueByName.computeIfAbsent(processName, name -> {
            AtomicLong holder = new AtomicLong();
            Gauge.builder("totrackit_processes_overdue_current", holder, AtomicLong::get)
                    .description("Active processes currently past their deadline")
                    .tag("process_name", name)
                    .register(meterRegistry);
            return holder;
        });
    }
}