package com.totrackit.service;

import com.totrackit.dto.ProcessFilter;
import com.totrackit.dto.Pageable;
import com.totrackit.dto.PagedResult;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.repository.ProcessRepository;
import com.totrackit.util.ProcessMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for ProcessService listProcesses method.
 */
public class ProcessServiceListTest {
    
    @Mock
    private ProcessRepository processRepository;
    
    @Mock
    private ProcessMapper processMapper;
    
    private ProcessService processService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processService = new ProcessService(processRepository, processMapper);
    }
    
    @Test
    public void testListProcesses_EmptyList() {
        // Given
        ProcessFilter filter = new ProcessFilter();
        Pageable pageable = new Pageable();
        
        when(processRepository.findWithComprehensiveFilters(null, null, null, Integer.MAX_VALUE, 0))
                .thenReturn(new ArrayList<>());
        
        // When
        PagedResult<ProcessResponse> result = processService.listProcesses(filter, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(0, result.getData().size());
        assertEquals(0, result.getTotal());
        assertEquals(20, result.getLimit());
        assertEquals(0, result.getOffset());
        assertFalse(result.isHasMore());
        
        verify(processRepository).findWithComprehensiveFilters(null, null, null, Integer.MAX_VALUE, 0);
    }
    
    @Test
    public void testListProcesses_WithProcesses() {
        // Given
        ProcessFilter filter = new ProcessFilter();
        Pageable pageable = new Pageable();
        
        List<ProcessEntity> entities = new ArrayList<>();
        ProcessEntity entity = new ProcessEntity("test-id", "test-process");
        entities.add(entity);
        
        ProcessResponse response = new ProcessResponse();
        response.setId("test-id");
        response.setName("test-process");
        
        when(processRepository.findWithComprehensiveFilters(null, null, null, Integer.MAX_VALUE, 0))
                .thenReturn(entities);
        when(processMapper.toResponse(entity)).thenReturn(response);
        
        // When
        PagedResult<ProcessResponse> result = processService.listProcesses(filter, pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getData().size());
        assertEquals(1, result.getTotal());
        assertEquals(20, result.getLimit());
        assertEquals(0, result.getOffset());
        assertFalse(result.isHasMore());
        
        ProcessResponse actualResponse = result.getData().get(0);
        assertEquals("test-id", actualResponse.getId());
        assertEquals("test-process", actualResponse.getName());
        
        verify(processRepository).findWithComprehensiveFilters(null, null, null, Integer.MAX_VALUE, 0);
        verify(processMapper).toResponse(entity);
    }
}