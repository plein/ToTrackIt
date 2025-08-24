package com.totrackit.controller;

import com.totrackit.dto.NewProcessRequest;
import com.totrackit.dto.PagedResult;
import com.totrackit.dto.ProcessResponse;
import com.totrackit.model.ProcessStatus;
import com.totrackit.service.ProcessService;
import io.micronaut.http.HttpResponse;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Test class for process listing functionality including filtering and sorting.
 */
@MicronautTest(startApplication = false)
public class ProcessListingTest {

    @Inject
    ProcessController processController;

    @Inject
    ProcessService processService;

    @BeforeEach
    void setUp() {
        Mockito.reset(processService);
    }

    @Test
    void testListProcesses_WithNameFilter() {
        // Arrange
        List<ProcessResponse> mockProcesses = Arrays.asList(
                createMockProcess("test-process", "id1", ProcessStatus.ACTIVE),
                createMockProcess("test-process", "id2", ProcessStatus.COMPLETED)
        );
        PagedResult<ProcessResponse> mockResult = new PagedResult<>(mockProcesses, 2L, 20, 0);
        
        when(processService.listProcesses(any(), any())).thenReturn(mockResult);

        // Act
        HttpResponse<PagedResult<ProcessResponse>> response = processController.listProcesses(
                "test-process", // name filter
                null, // id filter
                null, // status filter
                null, // deadline status filter
                null, // deadline before
                null, // deadline after
                null, // running duration min
                null, // sort_by
                null, // limit
                null, // offset
                null  // tags
        );

        // Assert
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.getBody().isPresent());
        PagedResult<ProcessResponse> result = response.getBody().get();
        assertEquals(2, result.getData().size());
        assertEquals(2L, result.getTotal());
    }

    @Test
    void testListProcesses_WithIdFilter() {
        // Arrange
        List<ProcessResponse> mockProcesses = Arrays.asList(
                createMockProcess("any-process", "specific-id", ProcessStatus.ACTIVE)
        );
        PagedResult<ProcessResponse> mockResult = new PagedResult<>(mockProcesses, 1L, 20, 0);
        
        when(processService.listProcesses(any(), any())).thenReturn(mockResult);

        // Act
        HttpResponse<PagedResult<ProcessResponse>> response = processController.listProcesses(
                null, // name filter
                "specific-id", // id filter
                null, // status filter
                null, // deadline status filter
                null, // deadline before
                null, // deadline after
                null, // running duration min
                null, // sort_by
                null, // limit
                null, // offset
                null  // tags
        );

        // Assert
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.getBody().isPresent());
        PagedResult<ProcessResponse> result = response.getBody().get();
        assertEquals(1, result.getData().size());
        assertEquals("specific-id", result.getData().get(0).getId());
    }

    @Test
    void testListProcesses_WithSortByParameter() {
        // Arrange
        List<ProcessResponse> mockProcesses = Arrays.asList(
                createMockProcess("process1", "id1", ProcessStatus.ACTIVE),
                createMockProcess("process2", "id2", ProcessStatus.COMPLETED)
        );
        PagedResult<ProcessResponse> mockResult = new PagedResult<>(mockProcesses, 2L, 20, 0);
        
        when(processService.listProcesses(any(), any())).thenReturn(mockResult);

        // Act
        HttpResponse<PagedResult<ProcessResponse>> response = processController.listProcesses(
                null, // name filter
                null, // id filter
                null, // status filter
                null, // deadline status filter
                null, // deadline before
                null, // deadline after
                null, // running duration min
                "name:asc,started_at:desc", // sort_by
                null, // limit
                null, // offset
                null  // tags
        );

        // Assert
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.getBody().isPresent());
        PagedResult<ProcessResponse> result = response.getBody().get();
        assertEquals(2, result.getData().size());
    }

    @Test
    void testListProcesses_WithMultipleFilters() {
        // Arrange
        List<ProcessResponse> mockProcesses = Arrays.asList(
                createMockProcess("test-process", "test-id", ProcessStatus.ACTIVE)
        );
        PagedResult<ProcessResponse> mockResult = new PagedResult<>(mockProcesses, 1L, 20, 0);
        
        when(processService.listProcesses(any(), any())).thenReturn(mockResult);

        // Act
        HttpResponse<PagedResult<ProcessResponse>> response = processController.listProcesses(
                "test-process", // name filter
                "test-id", // id filter
                ProcessStatus.ACTIVE, // status filter
                null, // deadline status filter
                null, // deadline before
                null, // deadline after
                null, // running duration min
                "started_at:desc", // sort_by
                10, // limit
                0, // offset
                null  // tags
        );

        // Assert
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.getBody().isPresent());
        PagedResult<ProcessResponse> result = response.getBody().get();
        assertEquals(1, result.getData().size());
        assertEquals("test-process", result.getData().get(0).getName());
        assertEquals("test-id", result.getData().get(0).getId());
        assertEquals(ProcessStatus.ACTIVE, result.getData().get(0).getStatus());
    }

    @Test
    void testListProcesses_WithTagFilter() {
        // Arrange
        List<ProcessResponse> mockProcesses = Arrays.asList(
                createMockProcess("test-process", "test-id", ProcessStatus.ACTIVE)
        );
        PagedResult<ProcessResponse> mockResult = new PagedResult<>(mockProcesses, 1L, 20, 0);
        
        when(processService.listProcesses(any(), any())).thenReturn(mockResult);

        // Act
        HttpResponse<PagedResult<ProcessResponse>> response = processController.listProcesses(
                null, // name filter
                null, // id filter
                null, // status filter
                null, // deadline status filter
                null, // deadline before
                null, // deadline after
                null, // running duration min
                null, // sort_by
                null, // limit
                null, // offset
                "environment:production"  // tags
        );

        // Assert
        assertEquals(200, response.getStatus().getCode());
        assertTrue(response.getBody().isPresent());
        PagedResult<ProcessResponse> result = response.getBody().get();
        assertEquals(1, result.getData().size());
    }

    @MockBean(ProcessService.class)
    ProcessService processService() {
        return Mockito.mock(ProcessService.class);
    }

    private ProcessResponse createMockProcess(String name, String id, ProcessStatus status) {
        ProcessResponse process = new ProcessResponse();
        process.setName(name);
        process.setId(id);
        process.setStatus(status);
        process.setStartedAt(Instant.now().getEpochSecond());
        return process;
    }
}