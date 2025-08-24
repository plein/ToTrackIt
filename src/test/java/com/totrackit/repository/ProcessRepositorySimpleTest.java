package com.totrackit.repository;

import com.totrackit.entity.ProcessEntity;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class ProcessRepositorySimpleTest {
    
    @Inject
    ProcessRepository processRepository;
    
    @Test
    public void testRepositoryExists() {
        assertNotNull(processRepository);
    }
    
    @Test
    public void testSaveProcess() {
        // Given
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        
        // When
        ProcessEntity saved = processRepository.save(entity);
        
        // Then
        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("test-id", saved.getProcessId());
        assertEquals("test-process", saved.getName());
    }
}