package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.totrackit.model.ProcessTag;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a new process.
 */
@Introspected
@Serdeable
public class NewProcessRequest {
    
    @NotBlank(message = "Process ID is required")
    @Size(min = 3, max = 50, message = "Process ID must be between 3 and 50 characters")
    @JsonProperty("id")
    private String id;
    
    @Min(value = 0, message = "Deadline must be a positive timestamp")
    @JsonProperty("deadline")
    private Long deadline; // Unix timestamp in seconds
    
    @Valid
    @JsonProperty("tags")
    private List<ProcessTag> tags;
    
    @JsonProperty("context")
    private Map<String, Object> context;
    
    // Default constructor
    public NewProcessRequest() {}
    
    // Constructor with required fields
    public NewProcessRequest(String id) {
        this.id = id;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    @Override
    public String toString() {
        return "NewProcessRequest{" +
                "id='" + id + '\'' +
                ", deadline=" + deadline +
                ", tags=" + tags +
                ", context=" + context +
                '}';
    }
}