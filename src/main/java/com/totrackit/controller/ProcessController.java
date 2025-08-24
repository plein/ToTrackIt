package com.totrackit.controller;

import com.totrackit.dto.CompleteProcessRequest;
import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.model.ProcessStatus;
import com.totrackit.service.ProcessService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.validation.Validated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import io.micronaut.core.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/processes")
@Validated
public class ProcessController {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProcessController.class);
    
    private final ProcessService processService;
    
    @Inject
    public ProcessController(ProcessService processService) {
        this.processService = processService;
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
}