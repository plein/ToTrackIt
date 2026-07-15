package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/**
 * Response for the tag-impact analytics endpoint: which tag segments the
 * current problems (overdue, late, failed processes) are concentrated in.
 */
@Introspected
@Serdeable
public class TagImpactResponse {

    @JsonProperty("window_hours")
    private int windowHours;

    @JsonProperty("generated_at")
    private long generatedAt; // Unix timestamp in seconds

    /** All processes considered (active, or finished within the window). */
    @JsonProperty("total_processes")
    private long totalProcesses;

    /** Processes currently overdue, completed late, or failed in the window. */
    @JsonProperty("problem_processes")
    private long problemProcesses;

    /** Completion-duration stats across all finished runs in the window. */
    @JsonProperty("duration")
    private DurationStats duration;

    @JsonProperty("tags")
    private List<TagImpactEntry> tags;

    public TagImpactResponse() {}

    public DurationStats getDuration() { return duration; }
    public void setDuration(DurationStats duration) { this.duration = duration; }

    public int getWindowHours() { return windowHours; }
    public void setWindowHours(int windowHours) { this.windowHours = windowHours; }

    public long getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(long generatedAt) { this.generatedAt = generatedAt; }

    public long getTotalProcesses() { return totalProcesses; }
    public void setTotalProcesses(long totalProcesses) { this.totalProcesses = totalProcesses; }

    public long getProblemProcesses() { return problemProcesses; }
    public void setProblemProcesses(long problemProcesses) { this.problemProcesses = problemProcesses; }

    public List<TagImpactEntry> getTags() { return tags; }
    public void setTags(List<TagImpactEntry> tags) { this.tags = tags; }
}
