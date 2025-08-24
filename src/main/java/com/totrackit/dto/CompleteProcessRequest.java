package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.totrackit.model.ProcessStatus;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;


/**
 * Request DTO for completing a process.
 */
@Introspected
@Serdeable
public class CompleteProcessRequest {
    
    @JsonProperty("status")
    private ProcessStatus status;
    
    // Default constructor
    public CompleteProcessRequest() {
        status = ProcessStatus.COMPLETED;
    }
    
    // Constructor with required fields
    public CompleteProcessRequest(ProcessStatus status) {
        this.status = status;
    }
    
    // Getters and setters
    public ProcessStatus getStatus() {
        return status;
    }
    
    public void setStatus(ProcessStatus status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "CompleteProcessRequest{" +
                "status=" + status +
                '}';
    }
}