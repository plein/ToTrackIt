package com.totrackit.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testNewProcessRequestSerialization() throws Exception {
        NewProcessRequest request = new NewProcessRequest("test-process-id");
        request.setDeadline(1640995200L); // 2022-01-01 00:00:00 UTC
        request.setTags(List.of(new ProcessTag("env", "prod")));
        request.setContext(Map.of("user_id", "123", "batch_size", 100));

        String json = objectMapper.writeValueAsString(request);
        assertNotNull(json);
        assertTrue(json.contains("test-process-id"));
        assertTrue(json.contains("1640995200"));

        NewProcessRequest deserialized = objectMapper.readValue(json, NewProcessRequest.class);
        assertEquals("test-process-id", deserialized.getId());
        assertEquals(1640995200L, deserialized.getDeadline());
        assertEquals(1, deserialized.getTags().size());
        assertEquals("env", deserialized.getTags().get(0).getKey());
    }

    @Test
    void testProcessResponseSerialization() throws Exception {
        ProcessResponse response = new ProcessResponse("test-id", "test-process", ProcessStatus.ACTIVE);
        response.setStartedAt(1640995200L);
        response.setDuration(3600L);
        response.setTags(List.of(new ProcessTag("team", "backend")));

        String json = objectMapper.writeValueAsString(response);
        assertNotNull(json);
        assertTrue(json.contains("test-id"));
        assertTrue(json.contains("ACTIVE"));

        ProcessResponse deserialized = objectMapper.readValue(json, ProcessResponse.class);
        assertEquals("test-id", deserialized.getId());
        assertEquals("test-process", deserialized.getName());
        assertEquals(ProcessStatus.ACTIVE, deserialized.getStatus());
        assertEquals(1640995200L, deserialized.getStartedAt());
    }

    @Test
    void testCompleteProcessRequestSerialization() throws Exception {
        CompleteProcessRequest request = new CompleteProcessRequest(ProcessStatus.COMPLETED);

        String json = objectMapper.writeValueAsString(request);
        assertNotNull(json);
        assertTrue(json.contains("COMPLETED"));

        CompleteProcessRequest deserialized = objectMapper.readValue(json, CompleteProcessRequest.class);
        assertEquals(ProcessStatus.COMPLETED, deserialized.getStatus());
    }
}