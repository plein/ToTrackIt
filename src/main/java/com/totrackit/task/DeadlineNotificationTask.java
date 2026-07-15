package com.totrackit.task;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.MetricsService;
import com.totrackit.service.WebhookNotificationService;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled scanner that detects active processes past their deadline.
 * Each breach is processed at most once (tracked via the deadline_notified_at
 * column): the deadline-missed metric is recorded and, when a webhook is
 * configured, a notification is sent.
 *
 * The scanner always runs; webhook delivery is optional. When a webhook is
 * configured, a process is only marked processed after successful delivery,
 * so failed deliveries are retried on the next scan.
 */
@Singleton
public class DeadlineNotificationTask {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineNotificationTask.class);

    private final ProcessRepository processRepository;
    private final MetricsService metricsService;
    @Nullable
    private final WebhookNotificationService notificationService;

    @Inject
    public DeadlineNotificationTask(ProcessRepository processRepository,
                                    MetricsService metricsService,
                                    @Nullable WebhookNotificationService notificationService) {
        this.processRepository = processRepository;
        this.metricsService = metricsService;
        this.notificationService = notificationService;
    }

    /**
     * Scans for overdue, unprocessed deadline breaches; records metrics and
     * notifies the webhook when one is configured.
     */
    @Scheduled(fixedDelay = "${totrackit.notification-scan-interval:60s}", initialDelay = "30s")
    public void notifyMissedDeadlines() {
        boolean webhookActive = notificationService != null && notificationService.isEnabled();
        try {
            List<ProcessEntity> overdue = processRepository.findOverdueUnnotified(Instant.now());
            if (overdue.isEmpty()) {
                return;
            }
            LOG.debug("Found {} overdue processes awaiting deadline processing", overdue.size());
            for (ProcessEntity process : overdue) {
                if (webhookActive && !notificationService.sendDeadlineMissed(process)) {
                    continue; // delivery failed; retried on the next scan
                }
                processRepository.markDeadlineNotified(process.getId(), Instant.now());
                metricsService.recordDeadlineMissed(process.getName());
            }
        } catch (Exception e) {
            LOG.warn("Deadline notification scan failed", e);
        }
    }
}
