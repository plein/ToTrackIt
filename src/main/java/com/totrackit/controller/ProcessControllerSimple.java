package com.totrackit.controller;

import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.service.ProcessServiceSimple;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.validation.Validated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/processes-simple")
@Validated
public class ProcessControllerSimple {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProcessControllerSimple.class);
    
    private final ProcessServiceSimple processService;
    
    @Inject
    public ProcessControllerSimple(ProcessServiceSimple processService) {
        this.processService = processService;
    }
    
    @Post("/{name}")
    public HttpResponse<ProcessResponse> createProcess(
            @PathVariable @NotBlank String name,
            @Body @Valid NewProcessRequest request) {
        
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
}