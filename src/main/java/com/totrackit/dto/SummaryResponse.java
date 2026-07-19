package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Workspace-wide headline stats, aggregated in one SQL pass. Powers the
 * dashboard stat cards, sidebar counts, and the Metrics page without the UI
 * having to page through the process list.
 */
@Introspected
@Serdeable
public class SummaryResponse {

    @JsonProperty("generated_at")
    private long generatedAt; // Unix timestamp in seconds

    @JsonProperty("total")
    private long total;

    @JsonProperty("active")
    private long active;

    @JsonProperty("completed")
    private long completed;

    @JsonProperty("failed")
    private long failed;

    /** Active processes currently past their deadline. */
    @JsonProperty("overdue")
    private long overdue;

    /** Active processes with a deadline still ahead. */
    @JsonProperty("on_track")
    private long onTrack;

    @JsonProperty("completed_on_time")
    private long completedOnTime;

    @JsonProperty("completed_late")
    private long completedLate;

    @JsonProperty("completed_24h")
    private long completed24h;

    @JsonProperty("completed_on_time_24h")
    private long completedOnTime24h;

    @JsonProperty("failed_24h")
    private long failed24h;

    public SummaryResponse() {}

    public long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(long generatedAt) { this.generatedAt = generatedAt; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getActive() { return active; }
    public void setActive(long active) { this.active = active; }

    public long getCompleted() { return completed; }
    public void setCompleted(long completed) { this.completed = completed; }

    public long getFailed() { return failed; }
    public void setFailed(long failed) { this.failed = failed; }

    public long getOverdue() { return overdue; }
    public void setOverdue(long overdue) { this.overdue = overdue; }

    public long getOnTrack() { return onTrack; }
    public void setOnTrack(long onTrack) { this.onTrack = onTrack; }

    public long getCompletedOnTime() { return completedOnTime; }
    public void setCompletedOnTime(long completedOnTime) { this.completedOnTime = completedOnTime; }

    public long getCompletedLate() { return completedLate; }
    public void setCompletedLate(long completedLate) { this.completedLate = completedLate; }

    public long getCompleted24h() { return completed24h; }
    public void setCompleted24h(long completed24h) { this.completed24h = completed24h; }

    public long getCompletedOnTime24h() { return completedOnTime24h; }
    public void setCompletedOnTime24h(long completedOnTime24h) { this.completedOnTime24h = completedOnTime24h; }

    public long getFailed24h() { return failed24h; }
    public void setFailed24h(long failed24h) { this.failed24h = failed24h; }
}
