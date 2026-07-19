package com.totrackit.task;

import com.totrackit.model.ProcessStatus;
import com.totrackit.repository.ProcessQueryRepository;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.MetricsService;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

/**
 * Scheduled task to update gauge metrics that need periodic refresh.
 * Updates metrics like active process count, database connection pool status, etc.
 */
@Singleton
public class MetricsUpdateTask {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsUpdateTask.class);

    /** Per-name overdue gauges are capped to this many process names. */
    private static final int MAX_OVERDUE_NAMES = 100;

    private final ProcessRepository processRepository;
    private final ProcessQueryRepository processQueryRepository;
    private final MetricsService metricsService;

    @Inject
    public MetricsUpdateTask(ProcessRepository processRepository,
                             ProcessQueryRepository processQueryRepository,
                             MetricsService metricsService) {
        this.processRepository = processRepository;
        this.processQueryRepository = processQueryRepository;
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

            // Per-name overdue gauges, aggregated in SQL (index-only over the
            // deadline partial index) instead of loading every overdue row.
            Map<String, Long> overdueByName =
                    processQueryRepository.countOverdueByName(Instant.now(), MAX_OVERDUE_NAMES);
            metricsService.updateOverdueProcessCounts(overdueByName);

            LOG.debug("Updated gauge metrics - active processes: {}, overdue names: {}",
                    activeProcessCount, overdueByName.size());

        } catch (Exception e) {
            LOG.warn("Failed to update gauge metrics", e);
        }
    }
}
