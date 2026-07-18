package com.totrackit.task;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.MetricsService;
import com.totrackit.service.WebhookNotificationService;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled scanner for deadline events. Each event is processed at most once
 * per process (tracked via the deadline_notified_at / deadline_warned_at
 * columns): the corresponding metric is recorded and, when a webhook is
 * configured, a notification is sent.
 *
 * Two events:
 * - process.deadline_warning: an active run crossed the warning threshold
 *   (fraction of its deadline budget, totrackit.warning-threshold, default
 *   0.75; values outside (0,1) disable warnings)
 * - process.deadline_missed: an active run passed its deadline
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
    private final double warningThreshold;

    @Inject
    public DeadlineNotificationTask(ProcessRepository processRepository,
                                    MetricsService metricsService,
                                    @Nullable WebhookNotificationService notificationService,
                                    @Value("${totrackit.warning-threshold:0.75}") double warningThreshold) {
        this.processRepository = processRepository;
        this.metricsService = metricsService;
        this.notificationService = notificationService;
        this.warningThreshold = warningThreshold;
    }

    /**
     * Scans for unprocessed deadline events; records metrics and notifies the
     * webhook when one is configured.
     */
    @Scheduled(fixedDelay = "${totrackit.notification-scan-interval:60s}", initialDelay = "30s")
    public void notifyMissedDeadlines() {
        boolean webhookActive = notificationService != null && notificationService.isEnabled();
        try {
            warnApproachingDeadlines(webhookActive);
            processMissedDeadlines(webhookActive);
        } catch (Exception e) {
            LOG.warn("Deadline notification scan failed", e);
        }
    }

    private void processMissedDeadlines(boolean webhookActive) {
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
    }

    private void warnApproachingDeadlines(boolean webhookActive) {
        if (warningThreshold <= 0 || warningThreshold >= 1) {
            return;
        }
        Instant now = Instant.now();
        for (ProcessEntity process : processRepository.findApproachingUnwarned(now)) {
            if (!hasCrossedWarningThreshold(process, now)) {
                continue;
            }
            long secondsRemaining = process.getDeadline().getEpochSecond() - now.getEpochSecond();
            if (webhookActive && !notificationService.sendDeadlineWarning(process, secondsRemaining)) {
                continue; // delivery failed; retried on the next scan
            }
            processRepository.markDeadlineWarned(process.getId(), now);
            metricsService.recordDeadlineWarning(process.getName());
        }
    }

    private boolean hasCrossedWarningThreshold(ProcessEntity process, Instant now) {
        if (process.getStartedAt() == null || process.getDeadline() == null) {
            return false;
        }
        long budget = process.getDeadline().getEpochSecond() - process.getStartedAt().getEpochSecond();
        if (budget <= 0) {
            return false;
        }
        long elapsed = now.getEpochSecond() - process.getStartedAt().getEpochSecond();
        return (double) elapsed / budget >= warningThreshold;
    }
}
