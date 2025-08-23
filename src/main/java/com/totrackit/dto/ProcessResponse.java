package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for process information.
 */
@Introspected
@Serdeable
public class ProcessResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("status")
    private ProcessStatus status;
    
    @JsonProperty("deadline_status")
    private DeadlineStatus deadlineStatus;
    
    @JsonProperty("started_at")
    private Long startedAt; // Unix timestamp in seconds
    
    @JsonProperty("completed_at")
    private Long completedAt; // Unix timestamp in seconds
    
    @JsonProperty("deadline")
    private Long deadline; // Unix timestamp in seconds
    
    @JsonProperty("tags")
    private List<ProcessTag> tags;
    
    @JsonProperty("context")
    private Map<String, Object> context;
    
    @JsonProperty("duration")
    private Long duration; // Duration in seconds
    
    // Default constructor
    public ProcessResponse() {}
    
    // Constructor with required fields
    public ProcessResponse(String id, String name, ProcessStatus status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public ProcessStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProcessStatus status) {
        this.status = status;
    }
    
    public DeadlineStatus getDeadlineStatus() {
        return deadlineStatus;
    }
    
    public void setDeadlineStatus(DeadlineStatus deadlineStatus) {
        this.deadlineStatus = deadlineStatus;
    }
    
    public Long getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Long startedAt) {
        this.startedAt = startedAt;
    }
    
    public Long getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }
    
    public Long getDeadline() {
        return deadline;
    }
    
    public void setDeadline(Long deadline) {
        this.deadline = deadline;
    }
    
    public List<ProcessTag> getTags() {
        return tags;
    }
    
    public void setTags(List<ProcessTag> tags) {
        this.tags = tags;
    }
    
    public Map<String, Object> getContext() {
        return context;
    }
    
    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
    
    public Long getDuration() {
        return duration;
    }
    
    public void setDuration(Long duration) {
        this.duration = duration;
    }
    
    @Override
    public String toString() {
        return "ProcessResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", deadlineStatus=" + deadlineStatus +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", deadline=" + deadline +
                ", duration=" + duration +
                '}';
    }
}