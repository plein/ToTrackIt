package com.totrackit.service;

import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.ProcessResponse;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class ProcessServiceSimpleTest {
    
    @Inject
    ProcessService processService;
    
    @Test
    public void testCreateProcess() {
        // Given
        String processName = "test-process";
        NewProcessRequest request = new NewProcessRequest("test-id-001");
        
        // When
        ProcessResponse response = processService.createProcess(processName, request);
        
        // Then
        assertNotNull(response);
        assertEquals("test-id-001", response.getId());
        assertEquals(processName, response.getName());
        assertNotNull(response.getStartedAt());
    }
}