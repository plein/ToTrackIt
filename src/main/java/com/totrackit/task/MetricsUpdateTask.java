package com.totrackit.task;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.MetricsService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

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

            // Update per-name overdue gauges (active processes past their deadline)
            Map<String, Long> overdueByName = processRepository.findOverdueProcesses(Instant.now()).stream()
                    .collect(Collectors.groupingBy(ProcessEntity::getName, Collectors.counting()));
            metricsService.updateOverdueProcessCounts(overdueByName);

            LOG.debug("Updated gauge metrics - active processes: {}, overdue names: {}",
                    activeProcessCount, overdueByName.size());
            
        } catch (Exception e) {
            LOG.warn("Failed to update gauge metrics", e);
        }
    }
}