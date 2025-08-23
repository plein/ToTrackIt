package com.totrackit.repository;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProcessRepository.
 * Tests basic repository functionality and bean injection.
 */
@MicronautTest
class ProcessRepositoryTest {
    
    @Inject
    ApplicationContext applicationContext;
    
    @Test
    void testRepositoryBeanExists() {
        // Test that the repository bean can be created
        assertTrue(applicationContext.containsBean(ProcessRepository.class));
    }
    
    @Test
    void testRepositoryCanBeInjected() {
        // Test that we can get the repository from the context
        ProcessRepository repository = applicationContext.getBean(ProcessRepository.class);
        assertNotNull(repository);
    }
    

}