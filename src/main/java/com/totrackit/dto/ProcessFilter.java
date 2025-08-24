package com.totrackit.dto;

import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import io.micronaut.core.annotation.Introspected;

import java.util.Map;

/**
 * Filter criteria for process listing operations.
 */
@Introspected
public class ProcessFilter {
    
    private ProcessStatus status;
    private DeadlineStatus deadlineStatus;
    private Map<String, String> tags;
    private Long deadlineBefore;
    private Long deadlineAfter;
    private Integer runningDurationMin;
    private String sortBy = "started_at";
    private String sortDirection = "desc";
    
    // Default constructor
    public ProcessFilter() {}
    
    // Getters and setters
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
    
    public Map<String, String> getTags() {
        return tags;
    }
    
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
    
    public Long getDeadlineBefore() {
        return deadlineBefore;
    }
    
    public void setDeadlineBefore(Long deadlineBefore) {
        this.deadlineBefore = deadlineBefore;
    }
    
    public Long getDeadlineAfter() {
        return deadlineAfter;
    }
    
    public void setDeadlineAfter(Long deadlineAfter) {
        this.deadlineAfter = deadlineAfter;
    }
    
    public Integer getRunningDurationMin() {
        return runningDurationMin;
    }
    
    public void setRunningDurationMin(Integer runningDurationMin) {
        this.runningDurationMin = runningDurationMin;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getSortDirection() {
        return sortDirection;
    }
    
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
    
    @Override
    public String toString() {
        return "ProcessFilter{" +
                "status=" + status +
                ", deadlineStatus=" + deadlineStatus +
                ", tags=" + tags +
                ", deadlineBefore=" + deadlineBefore +
                ", deadlineAfter=" + deadlineAfter +
                ", runningDurationMin=" + runningDurationMin +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                '}';
    }
}