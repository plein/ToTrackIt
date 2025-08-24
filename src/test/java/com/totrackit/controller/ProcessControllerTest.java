package com.totrackit.controller;

import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.exception.ProcessAlreadyExistsException;
import com.totrackit.service.ProcessService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProcessControllerTest {
    
    @Mock
    private ProcessService processService;
    
    @InjectMocks
    private ProcessController processController;
    
    @Test
    public void testCreateProcess_BasicSuccess() {
        // Given
        String processName = "test-process";
        NewProcessRequest request = new NewProcessRequest("test-id-001");
        
        ProcessResponse expectedResponse = new ProcessResponse();
        expectedResponse.setId("test-id-001");
        expectedResponse.setName(processName);
        expectedResponse.setStartedAt(System.currentTimeMillis() / 1000);
        
        when(processService.createProcess(eq(processName), any(NewProcessRequest.class)))
                .thenReturn(expectedResponse);
        
        // When
        HttpResponse<ProcessResponse> response = processController.createProcess(processName, request);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertNotNull(response.body());
        
        ProcessResponse processResponse = response.body();
        assertEquals("test-id-001", processResponse.getId());
        assertEquals(processName, processResponse.getName());
        assertNotNull(processResponse.getStartedAt());
    }
    
    @Test
    public void testCreateProcess_DuplicateProcess() {
        // Given
        String processName = "duplicate-process";
        NewProcessRequest request = new NewProcessRequest("duplicate-id");
        
        when(processService.createProcess(eq(processName), any(NewProcessRequest.class)))
                .thenThrow(new ProcessAlreadyExistsException(processName, "duplicate-id"));
        
        // When & Then
        ProcessAlreadyExistsException exception = assertThrows(ProcessAlreadyExistsException.class, () -> {
            processController.createProcess(processName, request);
        });
        
        assertTrue(exception.getMessage().contains("duplicate-process"));
        assertTrue(exception.getMessage().contains("duplicate-id"));
    }
    
    @Test
    public void testCreateProcess_WithDeadline() {
        // Given
        String processName = "deadline-process";
        NewProcessRequest request = new NewProcessRequest("deadline-id");
        request.setDeadline(System.currentTimeMillis() / 1000 + 3600); // 1 hour from now
        
        ProcessResponse expectedResponse = new ProcessResponse();
        expectedResponse.setId("deadline-id");
        expectedResponse.setName(processName);
        expectedResponse.setStartedAt(System.currentTimeMillis() / 1000);
        expectedResponse.setDeadline(request.getDeadline());
        
        when(processService.createProcess(eq(processName), any(NewProcessRequest.class)))
                .thenReturn(expectedResponse);
        
        // When
        HttpResponse<ProcessResponse> response = processController.createProcess(processName, request);
        
        // Then
        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertNotNull(response.body());
        
        ProcessResponse processResponse = response.body();
        assertEquals("deadline-id", processResponse.getId());
        assertEquals(processName, processResponse.getName());
        assertEquals(request.getDeadline(), processResponse.getDeadline());
    }
    
    @Test
    public void testCreateProcess_ServiceException() {
        // Given
        String processName = "error-process";
        NewProcessRequest request = new NewProcessRequest("error-id");
        
        when(processService.createProcess(eq(processName), any(NewProcessRequest.class)))
                .thenThrow(new RuntimeException("Service error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            processController.createProcess(processName, request);
        });
        
        assertEquals("Service error", exception.getMessage());
    }
}