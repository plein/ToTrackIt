package com.totrackit.repository;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Compilation test for ProcessRepository to verify all methods are properly defined.
 * This test doesn't require database setup and just verifies the interface compiles.
 */
class ProcessRepositoryCompilationTest {
    
    @Test
    void testRepositoryMethodsAreProperlyDefined() {
        // This test verifies that all repository methods compile correctly
        // by checking that the interface methods exist and have correct signatures
        
        // Verify the interface exists and extends CrudRepository
        Class<?> repositoryClass = ProcessRepository.class;
        assertNotNull(repositoryClass);
        
        // Verify key methods exist by checking they can be referenced
        try {
            // Basic CRUD methods from CrudRepository
            repositoryClass.getMethod("save", Object.class);
            repositoryClass.getMethod("findById", Object.class);
            repositoryClass.getMethod("deleteAll");
            
            // Custom query methods
            repositoryClass.getMethod("findByNameAndProcessId", String.class, String.class);
            repositoryClass.getMethod("existsActiveProcess", String.class, String.class);
            repositoryClass.getMethod("findWithFilters", ProcessStatus.class, int.class, int.class);
            repositoryClass.getMethod("findByNameAndStatus", String.class, ProcessStatus.class);
            repositoryClass.getMethod("findByTag", String.class, String.class);
            repositoryClass.getMethod("findByDeadlineBefore", Instant.class);
            repositoryClass.getMethod("findByDeadlineAfter", Instant.class);
            repositoryClass.getMethod("findOverdueProcesses", Instant.class);
            repositoryClass.getMethod("findByStatus", ProcessStatus.class);
            repositoryClass.getMethod("findByName", String.class);
            repositoryClass.getMethod("countByStatus", ProcessStatus.class);
            repositoryClass.getMethod("countAll");
            repositoryClass.getMethod("findCompletedProcesses");
            repositoryClass.getMethod("findWithTagFilters", ProcessStatus.class, String.class, int.class, int.class);
            
            // If we get here, all methods are properly defined
            assertTrue(true, "All repository methods are properly defined");
            
        } catch (NoSuchMethodException e) {
            fail("Repository method not found: " + e.getMessage());
        }
    }
    
    @Test
    void testProcessEntityCanBeInstantiated() {
        // Verify that ProcessEntity can be created (needed for repository operations)
        ProcessEntity entity = new ProcessEntity("test-id", "test-name");
        assertNotNull(entity);
        assertEquals("test-id", entity.getProcessId());
        assertEquals("test-name", entity.getName());
        assertEquals(ProcessStatus.ACTIVE, entity.getStatus());
    }
}