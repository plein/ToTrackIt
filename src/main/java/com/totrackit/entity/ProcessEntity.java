package com.totrackit.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JPA Entity representing a process in the database.
 */
@MappedEntity("processes")
@Introspected
public class ProcessEntity {
    
    @Id
    @GeneratedValue
    private Long id;
    
    @MappedProperty("process_id")
    private String processId;
    
    private String name;
    
    private ProcessStatus status = ProcessStatus.ACTIVE;
    
    @MappedProperty("started_at")
    private Instant startedAt;
    
    @MappedProperty("completed_at")
    private Instant completedAt;
    
    private Instant deadline;
    
    private String tags;
    
    private String context;
    
    @MappedProperty("created_at")
    private Instant createdAt;
    
    @MappedProperty("updated_at")
    private Instant updatedAt;
    
    // Default constructor
    public ProcessEntity() {
        Instant now = Instant.now();
        this.startedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    // Constructor with required fields
    public ProcessEntity(String processId, String name) {
        this();
        this.processId = processId;
        this.name = name;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getProcessId() {
        return processId;
    }
    
    public void setProcessId(String processId) {
        this.processId = processId;
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
        this.updatedAt = Instant.now();
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
        this.updatedAt = Instant.now();
    }
    
    public Instant getDeadline() {
        return deadline;
    }
    
    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public String getContext() {
        return context;
    }
    
    public void setContext(String context) {
        this.context = context;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Computed properties
    
    /**
     * Calculates the deadline status based on current state.
     */
    public DeadlineStatus getDeadlineStatus() {
        if (deadline == null) {
            return null;
        }
        
        Instant now = Instant.now();
        
        if (status == ProcessStatus.ACTIVE) {
            return now.isAfter(deadline) ? DeadlineStatus.MISSED : DeadlineStatus.ON_TRACK;
        } else if (status == ProcessStatus.COMPLETED) {
            return completedAt != null && completedAt.isBefore(deadline) || completedAt.equals(deadline) 
                ? DeadlineStatus.COMPLETED_ON_TIME 
                : DeadlineStatus.COMPLETED_LATE;
        }
        
        return null; // FAILED status or other cases
    }
    
    /**
     * Calculates the duration in seconds.
     * For active processes, returns current duration.
     * For completed processes, returns total duration.
     */
    public Long getDuration() {
        if (startedAt == null) {
            return null;
        }
        
        Instant endTime = completedAt != null ? completedAt : Instant.now();
        return endTime.getEpochSecond() - startedAt.getEpochSecond();
    }
    
    /**
     * Checks if the process is overdue (active and past deadline).
     */
    public boolean isOverdue() {
        return status == ProcessStatus.ACTIVE && 
               deadline != null && 
               Instant.now().isAfter(deadline);
    }
    
    // Helper methods for JSON serialization/deserialization
    
    /**
     * Converts tags JSON string to List of ProcessTag objects.
     */
    public List<ProcessTag> getTagsList() {
        if (tags == null || tags.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(tags, new TypeReference<List<ProcessTag>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
    
    /**
     * Converts List of ProcessTag objects to JSON string.
     */
    public void setTagsList(List<ProcessTag> tagsList) {
        if (tagsList == null || tagsList.isEmpty()) {
            this.tags = null;
            return;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.tags = mapper.writeValueAsString(tagsList);
        } catch (JsonProcessingException e) {
            this.tags = null;
        }
    }
    
    /**
     * Converts context JSON string to Map.
     */
    public Map<String, Object> getContextMap() {
        if (context == null || context.trim().isEmpty()) {
            return Map.of();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(context, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }
    
    /**
     * Converts Map to context JSON string.
     */
    public void setContextMap(Map<String, Object> contextMap) {
        if (contextMap == null || contextMap.isEmpty()) {
            this.context = null;
            return;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.context = mapper.writeValueAsString(contextMap);
        } catch (JsonProcessingException e) {
            this.context = null;
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessEntity that = (ProcessEntity) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ProcessEntity{" +
                "id=" + id +
                ", processId='" + processId + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", startedAt=" + startedAt +
                ", completedAt=" + completedAt +
                ", deadline=" + deadline +
                '}';
    }
}