package com.totrackit.service;

import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.exception.ProcessAlreadyExistsException;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.service.MetricsService;
import com.totrackit.util.ProcessMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for the improved create process pattern.
 * Tests the service logic for handling database constraint violations.
 */
class ProcessServiceCreatePatternTest {
    
    @Mock
    private ProcessRepository processRepository;
    
    @Mock
    private ProcessMapper processMapper;
    
    @Mock
    private MetricsService metricsService;
    
    private ProcessService processService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processService = new ProcessService(processRepository, processMapper, metricsService);
    }
    
    @Test
    void createProcess_SingleInsert_Success() {
        // Given
        String processName = "test-process";
        String processId = "unique-id";
        
        NewProcessRequest request = new NewProcessRequest();
        request.setId(processId);
        request.setDeadline(Instant.now().plusSeconds(3600).getEpochSecond());
        request.setTags(List.of(new ProcessTag("env", "test")));
        
        ProcessEntity savedEntity = new ProcessEntity(processId, processName);
        savedEntity.setId(1L);
        
        ProcessResponse expectedResponse = new ProcessResponse(processId, processName, ProcessStatus.ACTIVE);
        
        when(processRepository.save(any(ProcessEntity.class))).thenReturn(savedEntity);
        when(processMapper.toResponse(savedEntity)).thenReturn(expectedResponse);
        
        // When
        ProcessResponse response = processService.createProcess(processName, request);
        
        // Then
        assertNotNull(response);
        assertEquals(processId, response.getId());
        assertEquals(processName, response.getName());
        assertEquals(ProcessStatus.ACTIVE, response.getStatus());
        
        verify(processRepository, times(1)).save(any(ProcessEntity.class));
        verify(processMapper, times(1)).toResponse(savedEntity);
    }
    
    @Test
    void createProcess_UniqueConstraintViolation_ThrowsProcessAlreadyExistsException() {
        // Given
        String processName = "duplicate-test";
        String processId = "duplicate-id";
        
        NewProcessRequest request = new NewProcessRequest();
        request.setId(processId);
        
        // Simulate a database unique constraint violation
        RuntimeException dbException = new RuntimeException(
            "duplicate key value violates unique constraint \"idx_processes_unique_active\""
        );
        
        when(processRepository.save(any(ProcessEntity.class))).thenThrow(dbException);
        
        // When & Then
        ProcessAlreadyExistsException exception = assertThrows(
            ProcessAlreadyExistsException.class,
            () -> processService.createProcess(processName, request)
        );
        
        // Verify the exception message contains the process name and ID
        assertTrue(exception.getMessage().contains(processName));
        assertTrue(exception.getMessage().contains(processId));
        
        verify(processRepository, times(1)).save(any(ProcessEntity.class));
        verify(processMapper, never()).toResponse(any());
    }
    
    @Test
    void createProcess_GenericDataIntegrityViolation_ThrowsProcessAlreadyExistsException() {
        // Given
        String processName = "integrity-test";
        String processId = "integrity-id";
        
        NewProcessRequest request = new NewProcessRequest();
        request.setId(processId);
        
        // Simulate a generic data integrity violation
        RuntimeException dbException = new RuntimeException(
            "DataIntegrityViolationException: duplicate key value violates unique constraint"
        );
        
        when(processRepository.save(any(ProcessEntity.class))).thenThrow(dbException);
        
        // When & Then
        ProcessAlreadyExistsException exception = assertThrows(
            ProcessAlreadyExistsException.class,
            () -> processService.createProcess(processName, request)
        );
        
        assertTrue(exception.getMessage().contains(processName));
        assertTrue(exception.getMessage().contains(processId));
        
        verify(processRepository, times(1)).save(any(ProcessEntity.class));
    }
    
    @Test
    void createProcess_NonConstraintException_RethrowsOriginalException() {
        // Given
        String processName = "error-test";
        String processId = "error-id";
        
        NewProcessRequest request = new NewProcessRequest();
        request.setId(processId);
        
        // Simulate a non-constraint related exception
        RuntimeException dbException = new RuntimeException("Connection timeout");
        
        when(processRepository.save(any(ProcessEntity.class))).thenThrow(dbException);
        
        // When & Then
        RuntimeException exception = assertThrows(
            RuntimeException.class,
            () -> processService.createProcess(processName, request)
        );
        
        assertEquals("Connection timeout", exception.getMessage());
        assertNotEquals(ProcessAlreadyExistsException.class, exception.getClass());
        
        verify(processRepository, times(1)).save(any(ProcessEntity.class));
    }
}