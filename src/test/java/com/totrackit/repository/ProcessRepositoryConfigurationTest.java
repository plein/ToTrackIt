package com.totrackit.repository;

import com.totrackit.entity.ProcessEntity;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the ProcessRepository is properly configured and can be injected.
 * This test doesn't require database connectivity, just verifies the bean configuration.
 */
@MicronautTest(startApplication = false)
public class ProcessRepositoryConfigurationTest {
    
    @Inject
    ProcessRepository processRepository;
    
    @Test
    public void testRepositoryInjection() {
        // This test verifies that the repository can be injected
        // If the repository configuration is correct, this should not fail
        assertNotNull(processRepository, "ProcessRepository should be injectable");
    }
    
    @Test
    public void testRepositoryType() {
        // Verify that the repository is of the expected type
        assertTrue(processRepository instanceof ProcessRepository, 
                "Repository should implement ProcessRepository interface");
    }
}