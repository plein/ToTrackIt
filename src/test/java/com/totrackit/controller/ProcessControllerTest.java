package com.totrackit.controller;

import com.totrackit.dto.CompleteProcessRequest;
import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.exception.ProcessAlreadyCompletedException;
import com.totrackit.exception.ProcessAlreadyExistsException;
import com.totrackit.exception.ProcessNotFoundException;
import com.totrackit.model.ProcessStatus;
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
    
    @Test
    public void testCompleteProcess_Success() {
        // Given
        String processName = "test-process";
        String processId = "test-id-001";
        CompleteProcessRequest request = new CompleteProcessRequest(ProcessStatus.COMPLETED);
        
        ProcessResponse expectedResponse = new ProcessResponse();
        expectedResponse.setId(processId);
        expectedResponse.setName(processName);
        expectedResponse.setStatus(ProcessStatus.COMPLETED);
        expectedResponse.setStartedAt(System.currentTimeMillis() / 1000 - 3600); // Started 1 hour ago
        expectedResponse.setCompletedAt(System.currentTimeMillis() / 1000); // Completed now
        expectedResponse.setDuration(3600L); // 1 hour duration
        
        when(processService.completeProcess(eq(processName), eq(processId), eq(ProcessStatus.COMPLETED)))
                .thenReturn(expectedResponse);
        
        // When
        HttpResponse<ProcessResponse> response = processController.completeProcess(processName, processId, request);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        
        ProcessResponse processResponse = response.body();
        assertEquals(processId, processResponse.getId());
        assertEquals(processName, processResponse.getName());
        assertEquals(ProcessStatus.COMPLETED, processResponse.getStatus());
        assertNotNull(processResponse.getCompletedAt());
        assertNotNull(processResponse.getDuration());
    }
    
    @Test
    public void testCompleteProcess_WithFailedStatus() {
        // Given
        String processName = "failed-process";
        String processId = "failed-id";
        CompleteProcessRequest request = new CompleteProcessRequest(ProcessStatus.FAILED);
        
        ProcessResponse expectedResponse = new ProcessResponse();
        expectedResponse.setId(processId);
        expectedResponse.setName(processName);
        expectedResponse.setStatus(ProcessStatus.FAILED);
        expectedResponse.setStartedAt(System.currentTimeMillis() / 1000 - 1800); // Started 30 minutes ago
        expectedResponse.setCompletedAt(System.currentTimeMillis() / 1000); // Completed now
        expectedResponse.setDuration(1800L); // 30 minutes duration
        
        when(processService.completeProcess(eq(processName), eq(processId), eq(ProcessStatus.FAILED)))
                .thenReturn(expectedResponse);
        
        // When
        HttpResponse<ProcessResponse> response = processController.completeProcess(processName, processId, request);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        
        ProcessResponse processResponse = response.body();
        assertEquals(processId, processResponse.getId());
        assertEquals(processName, processResponse.getName());
        assertEquals(ProcessStatus.FAILED, processResponse.getStatus());
        assertNotNull(processResponse.getCompletedAt());
        assertNotNull(processResponse.getDuration());
    }
    
    @Test
    public void testCompleteProcess_ProcessNotFound() {
        // Given
        String processName = "nonexistent-process";
        String processId = "nonexistent-id";
        CompleteProcessRequest request = new CompleteProcessRequest(ProcessStatus.COMPLETED);
        
        when(processService.completeProcess(eq(processName), eq(processId), eq(ProcessStatus.COMPLETED)))
                .thenThrow(new ProcessNotFoundException(processName, processId));
        
        // When & Then
        ProcessNotFoundException exception = assertThrows(ProcessNotFoundException.class, () -> {
            processController.completeProcess(processName, processId, request);
        });
        
        assertTrue(exception.getMessage().contains("nonexistent-process"));
        assertTrue(exception.getMessage().contains("nonexistent-id"));
    }
    
    @Test
    public void testCompleteProcess_AlreadyCompleted() {
        // Given
        String processName = "completed-process";
        String processId = "completed-id";
        CompleteProcessRequest request = new CompleteProcessRequest(ProcessStatus.COMPLETED);
        
        when(processService.completeProcess(eq(processName), eq(processId), eq(ProcessStatus.COMPLETED)))
                .thenThrow(new ProcessAlreadyCompletedException(processName, processId));
        
        // When & Then
        ProcessAlreadyCompletedException exception = assertThrows(ProcessAlreadyCompletedException.class, () -> {
            processController.completeProcess(processName, processId, request);
        });
        
        assertTrue(exception.getMessage().contains("completed-process"));
        assertTrue(exception.getMessage().contains("completed-id"));
    }
    
    @Test
    public void testCompleteProcess_ServiceException() {
        // Given
        String processName = "error-process";
        String processId = "error-id";
        CompleteProcessRequest request = new CompleteProcessRequest(ProcessStatus.COMPLETED);
        
        when(processService.completeProcess(eq(processName), eq(processId), eq(ProcessStatus.COMPLETED)))
                .thenThrow(new RuntimeException("Service error"));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            processController.completeProcess(processName, processId, request);
        });
        
        assertEquals("Service error", exception.getMessage());
    }
    
    @Test
    public void testCompleteProcess_NoBodyDefaultsToCompleted() {
        // Given
        String processName = "test-process";
        String processId = "test-id-001";
        
        ProcessResponse expectedResponse = new ProcessResponse();
        expectedResponse.setId(processId);
        expectedResponse.setName(processName);
        expectedResponse.setStatus(ProcessStatus.COMPLETED);
        expectedResponse.setStartedAt(System.currentTimeMillis() / 1000 - 3600); // Started 1 hour ago
        expectedResponse.setCompletedAt(System.currentTimeMillis() / 1000); // Completed now
        expectedResponse.setDuration(3600L); // 1 hour duration
        
        when(processService.completeProcess(eq(processName), eq(processId), eq(ProcessStatus.COMPLETED)))
                .thenReturn(expectedResponse);
        
        // When - passing null as request body
        HttpResponse<ProcessResponse> response = processController.completeProcess(processName, processId, null);
        
        // Then
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        
        ProcessResponse processResponse = response.body();
        assertEquals(processId, processResponse.getId());
        assertEquals(processName, processResponse.getName());
        assertEquals(ProcessStatus.COMPLETED, processResponse.getStatus());
        assertNotNull(processResponse.getCompletedAt());
        assertNotNull(processResponse.getDuration());
    }
}