package com.totrackit.entity;

import com.totrackit.model.ProcessStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import java.time.Instant;
import java.util.Objects;

/**
 * Minimal JPA Entity representing a process in the database.
 * This version excludes all computed properties to avoid persistence issues.
 */
@MappedEntity("processes")
@Introspected
public class ProcessEntityMinimal {
    
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
    public ProcessEntityMinimal() {
        Instant now = Instant.now();
        this.startedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    // Constructor with required fields
    public ProcessEntityMinimal(String processId, String name) {
        this();
        this.processId = processId;
        this.name = name;
    }
    
    // Getters and setters - only basic properties
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessEntityMinimal that = (ProcessEntityMinimal) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "ProcessEntityMinimal{" +
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