package com.totrackit.task;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.AdvisoryLockService;
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
import java.util.ArrayList;
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
 * so failed deliveries are retried on the next scan. Each pass is bounded by
 * totrackit.notification-batch-size and aborts early after consecutive
 * delivery failures, so one slow or dead webhook endpoint cannot pin a scan
 * cycle to the size of the backlog. A cluster-wide advisory lock ensures at
 * most one replica scans at a time (webhooks would otherwise double-fire).
 */
@Singleton
public class DeadlineNotificationTask {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineNotificationTask.class);

    /** Cluster-wide advisory lock key for the deadline scan ("TOTRACKI"). */
    private static final long SCAN_LOCK_KEY = 0x544F545241434B49L;

    /** Abort the current pass after this many consecutive failed deliveries. */
    private static final int MAX_CONSECUTIVE_DELIVERY_FAILURES = 5;

    private final ProcessRepository processRepository;
    private final MetricsService metricsService;
    private final AdvisoryLockService advisoryLockService;
    @Nullable
    private final WebhookNotificationService notificationService;
    private final double warningThreshold;
    private final int batchSize;

    @Inject
    public DeadlineNotificationTask(ProcessRepository processRepository,
                                    MetricsService metricsService,
                                    AdvisoryLockService advisoryLockService,
                                    @Nullable WebhookNotificationService notificationService,
                                    @Value("${totrackit.warning-threshold:0.75}") double warningThreshold,
                                    @Value("${totrackit.notification-batch-size:500}") int batchSize) {
        this.processRepository = processRepository;
        this.metricsService = metricsService;
        this.advisoryLockService = advisoryLockService;
        this.notificationService = notificationService;
        this.warningThreshold = warningThreshold;
        this.batchSize = batchSize;
    }

    /**
     * Scans for unprocessed deadline events; records metrics and notifies the
     * webhook when one is configured.
     */
    @Scheduled(fixedDelay = "${totrackit.notification-scan-interval:60s}", initialDelay = "30s")
    public void notifyMissedDeadlines() {
        boolean webhookActive = notificationService != null && notificationService.isEnabled();
        try {
            boolean ran = advisoryLockService.runExclusive(SCAN_LOCK_KEY, () -> {
                warnApproachingDeadlines(webhookActive);
                processMissedDeadlines(webhookActive);
                updateBacklogGauges();
            });
            if (!ran) {
                LOG.debug("Deadline scan lock held by another replica; skipping this cycle");
            }
        } catch (Exception e) {
            LOG.warn("Deadline notification scan failed", e);
        }
    }

    private void processMissedDeadlines(boolean webhookActive) {
        List<ProcessEntity> overdue = processRepository.findOverdueUnnotified(Instant.now(), batchSize);
        if (overdue.isEmpty()) {
            return;
        }
        LOG.debug("Processing {} overdue processes awaiting deadline events", overdue.size());
        List<Long> processedIds = new ArrayList<>();
        int consecutiveFailures = 0;
        for (ProcessEntity process : overdue) {
            if (webhookActive && !notificationService.sendDeadlineMissed(process)) {
                if (++consecutiveFailures >= MAX_CONSECUTIVE_DELIVERY_FAILURES) {
                    LOG.warn("Aborting missed-deadline pass after {} consecutive delivery failures; "
                            + "remaining rows retry next scan", consecutiveFailures);
                    break;
                }
                continue; // delivery failed; retried on the next scan
            }
            consecutiveFailures = 0;
            processedIds.add(process.getId());
            metricsService.recordDeadlineMissed(process.getName());
        }
        if (!processedIds.isEmpty()) {
            processRepository.markDeadlineNotifiedBatch(processedIds, Instant.now());
        }
    }

    private void warnApproachingDeadlines(boolean webhookActive) {
        if (warningThreshold <= 0 || warningThreshold >= 1) {
            return;
        }
        Instant now = Instant.now();
        List<ProcessEntity> approaching = processRepository.findApproachingUnwarned(now, warningThreshold, batchSize);
        if (approaching.isEmpty()) {
            return;
        }
        List<Long> processedIds = new ArrayList<>();
        int consecutiveFailures = 0;
        for (ProcessEntity process : approaching) {
            long secondsRemaining = process.getDeadline().getEpochSecond() - now.getEpochSecond();
            if (webhookActive && !notificationService.sendDeadlineWarning(process, secondsRemaining)) {
                if (++consecutiveFailures >= MAX_CONSECUTIVE_DELIVERY_FAILURES) {
                    LOG.warn("Aborting deadline-warning pass after {} consecutive delivery failures; "
                            + "remaining rows retry next scan", consecutiveFailures);
                    break;
                }
                continue; // delivery failed; retried on the next scan
            }
            consecutiveFailures = 0;
            processedIds.add(process.getId());
            metricsService.recordDeadlineWarning(process.getName());
        }
        if (!processedIds.isEmpty()) {
            processRepository.markDeadlineWarnedBatch(processedIds, now);
        }
    }

    /**
     * Publishes how many deadline events are still unprocessed, so operators
     * can alert on a webhook endpoint that keeps failing.
     */
    private void updateBacklogGauges() {
        Instant now = Instant.now();
        long missedBacklog = processRepository.countOverdueUnnotified(now);
        long warningBacklog = (warningThreshold > 0 && warningThreshold < 1)
                ? processRepository.countApproachingUnwarned(now, warningThreshold)
                : 0;
        metricsService.updateNotificationBacklog(missedBacklog, warningBacklog);
    }
}
