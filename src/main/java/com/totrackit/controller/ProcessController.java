package com.totrackit.controller;

import com.totrackit.dto.CompleteProcessRequest;
import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.dto.ProcessFilter;
import com.totrackit.dto.Pageable;
import com.totrackit.dto.PagedResult;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.service.ProcessService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.validation.Validated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import io.micronaut.core.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

@Controller("/processes")
@Validated
public class ProcessController {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProcessController.class);
    
    private final ProcessService processService;
    
    @Inject
    public ProcessController(ProcessService processService) {
        this.processService = processService;
    }
    
    @Get("/")
    public HttpResponse<PagedResult<ProcessResponse>> listProcesses(
            @QueryValue @Nullable String name,
            @QueryValue @Nullable String id,
            @QueryValue @Nullable ProcessStatus status,
            @QueryValue @Nullable DeadlineStatus deadlineStatus,
            @QueryValue @Nullable Long deadlineBefore,
            @QueryValue @Nullable Long deadlineAfter,
            @QueryValue @Nullable Integer runningDurationMin,
            @QueryValue("sort_by") @Nullable String sortBy,
            @QueryValue @Nullable @Min(1) @Max(100) Integer limit,
            @QueryValue @Nullable @Min(0) Integer offset,
            @QueryValue("tags") @Nullable String tags) {
        
        LOG.info("Listing processes with filters: name={}, id={}, status={}, deadlineStatus={}, tags={}, limit={}, offset={}", 
                name, id, status, deadlineStatus, tags, limit, offset);
        
        try {
            // Build filter object
            ProcessFilter filter = new ProcessFilter();
            filter.setName(name);
            filter.setId(id);
            filter.setStatus(status);
            filter.setDeadlineStatus(deadlineStatus);
            filter.setDeadlineBefore(deadlineBefore);
            filter.setDeadlineAfter(deadlineAfter);
            filter.setRunningDurationMin(runningDurationMin);
            
            // Parse sort_by parameter (format: "field1:asc,field2:desc")
            parseSortParameter(filter, sortBy);
            
            // Parse tags parameter (format: "key1:value1,key2:value2")
            parseTagsParameter(filter, tags);
            
            // Build pagination object
            Pageable pageable = new Pageable(
                limit != null ? limit : 20,
                offset != null ? offset : 0
            );
            
            PagedResult<ProcessResponse> result = processService.listProcesses(filter, pageable);
            
            LOG.info("Successfully listed {} processes (total: {})", 
                    result.getData().size(), result.getTotal());
            
            return HttpResponse.ok(result);
            
        } catch (Exception e) {
            LOG.error("Failed to list processes with filters: status={}, deadlineStatus={}", 
                    status, deadlineStatus, e);
            throw e;
        }
    }
    
    @Post("/{name}")
    public HttpResponse<ProcessResponse> createProcess(
            @PathVariable 
            @NotBlank(message = "Process name is required")
            @Size(min = 1, max = 100, message = "Process name must be between 1 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Process name can only contain letters, numbers, underscores, and hyphens")
            String name,
            
            @Body 
            @Valid 
            NewProcessRequest request) {
        
        LOG.info("Creating process: name='{}', request={}", name, request);
        
        try {
            ProcessResponse response = processService.createProcess(name, request);
            
            LOG.info("Successfully created process: name='{}', id='{}'", name, response.getId());
            
            return HttpResponse.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            LOG.error("Failed to create process: name='{}', request={}", name, request, e);
            throw e;
        }
    }
    
    @Get("/{name}/{id}")
    public HttpResponse<ProcessResponse> getProcess(
            @PathVariable 
            @NotBlank(message = "Process name is required")
            @Size(min = 1, max = 100, message = "Process name must be between 1 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Process name can only contain letters, numbers, underscores, and hyphens")
            String name,
            
            @PathVariable("id")
            @NotBlank(message = "Process ID is required")
            @Size(min = 1, max = 50, message = "Process ID must be between 1 and 50 characters")
            String processId) {
        
        LOG.info("Retrieving process: name='{}', id='{}'", name, processId);
        
        try {
            ProcessResponse response = processService.getProcess(name, processId);
            
            LOG.info("Successfully retrieved process: name='{}', id='{}', status={}", 
                    name, processId, response.getStatus());
            
            return HttpResponse.ok(response);
            
        } catch (Exception e) {
            LOG.error("Failed to retrieve process: name='{}', id='{}'", name, processId, e);
            throw e;
        }
    }
    
    @Put("/{name}/{id}/complete")
    public HttpResponse<ProcessResponse> completeProcess(
            @PathVariable 
            @NotBlank(message = "Process name is required")
            @Size(min = 1, max = 100, message = "Process name must be between 1 and 100 characters")
            @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Process name can only contain letters, numbers, underscores, and hyphens")
            String name,
            
            @PathVariable("id")
            @NotBlank(message = "Process ID is required")
            @Size(min = 1, max = 50, message = "Process ID must be between 1 and 50 characters")
            String processId,
            
            @Body 
            @Nullable
            @Valid 
            CompleteProcessRequest request) {
        
        // Default to COMPLETED status if no body is provided
        ProcessStatus status = (request != null) ? request.getStatus() : ProcessStatus.COMPLETED;
        
        LOG.info("Completing process: name='{}', id='{}', status={}", name, processId, status);
        
        try {
            ProcessResponse response = processService.completeProcess(name, processId, status);
            
            LOG.info("Successfully completed process: name='{}', id='{}', status={}", 
                    name, processId, status);
            
            return HttpResponse.ok(response);
            
        } catch (Exception e) {
            LOG.error("Failed to complete process: name='{}', id='{}', status={}", 
                    name, processId, status, e);
            throw e;
        }
    }
    
    /**
     * Parses the sort_by parameter and sets the appropriate sort fields in the filter.
     * Format: "field1:asc,field2:desc" or just "field1,field2" (defaults to asc)
     * For now, we only support single field sorting, so we take the first field.
     */
    private void parseSortParameter(ProcessFilter filter, String sortBy) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            filter.setSortBy("started_at");
            filter.setSortDirection("desc");
            return;
        }
        
        // Split by comma and take the first field
        String[] sortFields = sortBy.split(",");
        String firstField = sortFields[0].trim();
        
        // Check if direction is specified
        if (firstField.contains(":")) {
            String[] parts = firstField.split(":");
            filter.setSortBy(parts[0].trim());
            filter.setSortDirection(parts.length > 1 ? parts[1].trim() : "asc");
        } else {
            filter.setSortBy(firstField);
            filter.setSortDirection("asc");
        }
        
        LOG.debug("Parsed sort parameter '{}' to sortBy='{}', sortDirection='{}'", 
                sortBy, filter.getSortBy(), filter.getSortDirection());
    }
    
    /**
     * Parses the tags parameter and sets the appropriate tag filters in the filter.
     * Format: "key1:value1,key2:value2" 
     * For simplicity, we only support the first tag for filtering.
     */
    private void parseTagsParameter(ProcessFilter filter, String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            filter.setTags(null);
            return;
        }
        
        try {
            // Split by comma and take the first tag
            String[] tagPairs = tags.split(",");
            String firstTag = tagPairs[0].trim();
            
            // Check if key:value format is used
            if (firstTag.contains(":")) {
                String[] parts = firstTag.split(":", 2);
                if (parts.length == 2) {
                    java.util.Map<String, String> tagMap = new HashMap<>();
                    tagMap.put(parts[0].trim(), parts[1].trim());
                    filter.setTags(tagMap);
                    
                    LOG.debug("Parsed tags parameter '{}' to tag filter: {}={}", 
                            tags, parts[0].trim(), parts[1].trim());
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse tags parameter '{}': {}", tags, e.getMessage());
            filter.setTags(null);
        }
    }
}