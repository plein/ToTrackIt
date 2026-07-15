package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Aggregated deadline outcomes for a single tag key/value pair, used to show
 * which segments (country, locale, provider, ...) a problem is concentrated in.
 */
@Introspected
@Serdeable
public class TagImpactEntry {

    @JsonProperty("key")
    private String key;

    @JsonProperty("value")
    private String value;

    /** Active processes currently past their deadline. */
    @JsonProperty("overdue")
    private long overdue;

    /** Active processes still within their deadline (or without one). */
    @JsonProperty("on_track")
    private long onTrack;

    /** Processes completed after their deadline within the window. */
    @JsonProperty("completed_late")
    private long completedLate;

    /** Processes completed within their deadline within the window. */
    @JsonProperty("completed_on_time")
    private long completedOnTime;

    /** Processes that failed within the window. */
    @JsonProperty("failed")
    private long failed;

    /** All processes carrying this tag seen in the window. */
    @JsonProperty("total")
    private long total;

    /** overdue + completed_late + failed; the sort key for impact. */
    @JsonProperty("problems")
    private long problems;

    /** Completion-duration stats for this tag's finished runs; null when none. */
    @JsonProperty("duration")
    private DurationStats duration;

    public TagImpactEntry() {}

    public TagImpactEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public long getOverdue() { return overdue; }
    public void setOverdue(long overdue) { this.overdue = overdue; }

    public long getOnTrack() { return onTrack; }
    public void setOnTrack(long onTrack) { this.onTrack = onTrack; }

    public long getCompletedLate() { return completedLate; }
    public void setCompletedLate(long completedLate) { this.completedLate = completedLate; }

    public long getCompletedOnTime() { return completedOnTime; }
    public void setCompletedOnTime(long completedOnTime) { this.completedOnTime = completedOnTime; }

    public long getFailed() { return failed; }
    public void setFailed(long failed) { this.failed = failed; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getProblems() { return problems; }
    public void setProblems(long problems) { this.problems = problems; }

    public DurationStats getDuration() { return duration; }
    public void setDuration(DurationStats duration) { this.duration = duration; }
}
