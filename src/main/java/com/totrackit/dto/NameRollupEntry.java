package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Aggregated run counts for one process name, computed with GROUP BY in SQL
 * so the by-name view never loads individual runs upfront.
 */
@Introspected
@Serdeable
public class NameRollupEntry {

    @JsonProperty("name")
    private String name;

    @JsonProperty("total")
    private long total;

    @JsonProperty("active")
    private long active;

    @JsonProperty("failed")
    private long failed;

    /** Active runs currently past their deadline. */
    @JsonProperty("overdue")
    private long overdue;

    @JsonProperty("completed")
    private long completed;

    @JsonProperty("completed_on_time")
    private long completedOnTime;

    @JsonProperty("completed_late")
    private long completedLate;

    @JsonProperty("last_started_at")
    private Long lastStartedAt; // Unix timestamp in seconds

    public NameRollupEntry() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getActive() { return active; }
    public void setActive(long active) { this.active = active; }

    public long getFailed() { return failed; }
    public void setFailed(long failed) { this.failed = failed; }

    public long getOverdue() { return overdue; }
    public void setOverdue(long overdue) { this.overdue = overdue; }

    public long getCompleted() { return completed; }
    public void setCompleted(long completed) { this.completed = completed; }

    public long getCompletedOnTime() { return completedOnTime; }
    public void setCompletedOnTime(long completedOnTime) { this.completedOnTime = completedOnTime; }

    public long getCompletedLate() { return completedLate; }
    public void setCompletedLate(long completedLate) { this.completedLate = completedLate; }

    public Long getLastStartedAt() { return lastStartedAt; }
    public void setLastStartedAt(Long lastStartedAt) { this.lastStartedAt = lastStartedAt; }
}
