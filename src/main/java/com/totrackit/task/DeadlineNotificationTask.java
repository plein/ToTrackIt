package com.totrackit.task;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.WebhookNotificationService;
import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled scanner that detects active processes past their deadline and
 * sends a webhook notification for each, at most once per process
 * (tracked via the deadline_notified_at column).
 *
 * Only active when webhook notifications are configured; see
 * {@link WebhookNotificationService}.
 */
@Singleton
@Requires(bean = WebhookNotificationService.class)
public class DeadlineNotificationTask {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineNotificationTask.class);

    private final ProcessRepository processRepository;
    private final WebhookNotificationService notificationService;

    @Inject
    public DeadlineNotificationTask(ProcessRepository processRepository,
                                    WebhookNotificationService notificationService) {
        this.processRepository = processRepository;
        this.notificationService = notificationService;
    }

    /**
     * Scans for overdue, unnotified processes and notifies the webhook.
     * A process is only marked notified after successful delivery, so failed
     * deliveries are retried on the next scan.
     */
    @Scheduled(fixedDelay = "${totrackit.notification-scan-interval:60s}", initialDelay = "30s")
    public void notifyMissedDeadlines() {
        if (!notificationService.isEnabled()) {
            return;
        }
        try {
            List<ProcessEntity> overdue = processRepository.findOverdueUnnotified(Instant.now());
            if (overdue.isEmpty()) {
                return;
            }
            LOG.debug("Found {} overdue processes awaiting deadline notification", overdue.size());
            for (ProcessEntity process : overdue) {
                if (notificationService.sendDeadlineMissed(process)) {
                    processRepository.markDeadlineNotified(process.getId(), Instant.now());
                }
            }
        } catch (Exception e) {
            LOG.warn("Deadline notification scan failed", e);
        }
    }
}
