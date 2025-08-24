package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.List;

@Introspected
@Serdeable
public class ErrorResponse {
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @JsonProperty("path")
    private String path;
    
    @JsonProperty("details")
    private List<ValidationError> details;
    
    public ErrorResponse() {
        this.timestamp = Instant.now().getEpochSecond();
    }
    
    public ErrorResponse(String error, String message) {
        this();
        this.error = error;
        this.message = message;
    }
    
    public ErrorResponse(String error, String message, String path) {
        this(error, message);
        this.path = path;
    }
    
    public ErrorResponse(String error, String message, String path, List<ValidationError> details) {
        this(error, message, path);
        this.details = details;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public List<ValidationError> getDetails() {
        return details;
    }
    
    public void setDetails(List<ValidationError> details) {
        this.details = details;
    }
}