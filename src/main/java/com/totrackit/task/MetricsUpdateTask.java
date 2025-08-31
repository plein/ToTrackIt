package com.totrackit.task;

import com.totrackit.model.ProcessStatus;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.MetricsService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled task to update gauge metrics that need periodic refresh.
 * Updates metrics like active process count, database connection pool status, etc.
 */
@Singleton
public class MetricsUpdateTask {
    
    private static final Logger LOG = LoggerFactory.getLogger(MetricsUpdateTask.class);
    
    private final ProcessRepository processRepository;
    private final MetricsService metricsService;
    
    @Inject
    public MetricsUpdateTask(ProcessRepository processRepository, MetricsService metricsService) {
        this.processRepository = processRepository;
        this.metricsService = metricsService;
    }
    
    /**
     * Updates gauge metrics every 30 seconds.
     * This includes active process count and other real-time metrics.
     */
    @Scheduled(fixedDelay = "30s", initialDelay = "10s")
    public void updateGaugeMetrics() {
        try {
            // Update active processes count
            long activeProcessCount = processRepository.countByStatus(ProcessStatus.ACTIVE);
            metricsService.recordActiveProcessesCount(activeProcessCount);
            
            LOG.debug("Updated gauge metrics - active processes: {}", activeProcessCount);
            
        } catch (Exception e) {
            LOG.warn("Failed to update gauge metrics", e);
        }
    }
}