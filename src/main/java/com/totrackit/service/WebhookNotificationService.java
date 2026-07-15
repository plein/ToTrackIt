package com.totrackit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.totrackit.entity.ProcessEntity;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends process lifecycle notifications to a single operator-configured webhook.
 *
 * Only active when the {@code totrackit.webhook-url} property (or the
 * {@code TOTRACKIT_WEBHOOK_URL} environment variable) is set. The webhook
 * receives a JSON POST for each event; any 2xx response counts as delivered.
 */
// NOTE: @Requires(property=...) without notEquals — notEquals is also satisfied
// when the property is absent, which would activate this bean with an
// unresolvable @Value placeholder. Blank values are handled via isEnabled().
@Singleton
@Requires(property = WebhookNotificationService.WEBHOOK_URL_PROPERTY)
public class WebhookNotificationService {

    public static final String WEBHOOK_URL_PROPERTY = "totrackit.webhook-url";

    private static final Logger LOG = LoggerFactory.getLogger(WebhookNotificationService.class);

    private final String webhookUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebhookNotificationService(@Value("${" + WEBHOOK_URL_PROPERTY + "}") String webhookUrl) throws Exception {
        this.webhookUrl = webhookUrl;
        if (webhookUrl == null || webhookUrl.isBlank()) {
            this.httpClient = null;
            LOG.warn("{} is set but blank - webhook notifications are DISABLED", WEBHOOK_URL_PROPERTY);
        } else {
            this.httpClient = HttpClient.create(new URL(webhookUrl));
            LOG.info("Webhook notifications enabled, target: {}", webhookUrl);
        }
    }

    /**
     * Whether a usable webhook URL is configured.
     */
    public boolean isEnabled() {
        return httpClient != null;
    }

    /**
     * Notifies the webhook that a process missed its deadline.
     *
     * @param process the overdue process
     * @return true if the webhook accepted the notification (2xx response)
     */
    public boolean sendDeadlineMissed(ProcessEntity process) {
        if (!isEnabled()) {
            return false;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "process.deadline_missed");
        payload.put("name", process.getName());
        payload.put("id", process.getProcessId());
        payload.put("started_at", process.getStartedAt() != null ? process.getStartedAt().getEpochSecond() : null);
        payload.put("deadline", process.getDeadline() != null ? process.getDeadline().getEpochSecond() : null);
        payload.put("tags", parseJsonQuietly(process.getTags()));
        payload.put("context", parseJsonQuietly(process.getContext()));

        try {
            HttpResponse<?> response = httpClient.toBlocking().exchange(
                    HttpRequest.POST(webhookUrl, payload).contentType(MediaType.APPLICATION_JSON_TYPE));
            boolean delivered = response.getStatus().getCode() >= 200 && response.getStatus().getCode() < 300;
            if (!delivered) {
                LOG.warn("Webhook returned non-success status {} for process {}/{}",
                        response.getStatus().getCode(), process.getName(), process.getProcessId());
            }
            return delivered;
        } catch (Exception e) {
            LOG.warn("Failed to deliver deadline notification for process {}/{}: {}",
                    process.getName(), process.getProcessId(), e.getMessage());
            return false;
        }
    }

    private Object parseJsonQuietly(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Object>() {});
        } catch (Exception e) {
            return json;
        }
    }

    @PreDestroy
    void close() {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
