package com.totrackit.controller;

import com.totrackit.dto.NewProcessRequest;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for ProcessController validation and error handling.
 * This test focuses on validation logic without requiring database connectivity.
 */
@MicronautTest
public class ProcessControllerIntegrationTest {
    
    @Inject
    @Client("/")
    HttpClient client;
    
    @Test
    public void testCreateProcess_ValidationError_EmptyId() {
        // Given - invalid request with empty ID
        String processName = "validation-test";
        NewProcessRequest request = new NewProcessRequest("");
        
        // When & Then
        HttpRequest<NewProcessRequest> httpRequest = HttpRequest.POST("/processes/" + processName, request);
        
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(httpRequest, String.class);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
    
    @Test
    public void testCreateProcess_ValidationError_ShortId() {
        // Given - invalid request with ID too short
        String processName = "validation-test";
        NewProcessRequest request = new NewProcessRequest("ab"); // Less than 3 characters
        
        // When & Then
        HttpRequest<NewProcessRequest> httpRequest = HttpRequest.POST("/processes/" + processName, request);
        
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(httpRequest, String.class);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
    
    @Test
    public void testCreateProcess_ValidationError_LongId() {
        // Given - invalid request with ID too long
        String processName = "validation-test";
        String longId = "a".repeat(51); // More than 50 characters
        NewProcessRequest request = new NewProcessRequest(longId);
        
        // When & Then
        HttpRequest<NewProcessRequest> httpRequest = HttpRequest.POST("/processes/" + processName, request);
        
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(httpRequest, String.class);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
    
    @Test
    public void testCreateProcess_ValidationError_InvalidProcessName() {
        // Given - invalid process name with special characters
        String processName = "test@process!";
        NewProcessRequest request = new NewProcessRequest("valid-id");
        
        // When & Then
        HttpRequest<NewProcessRequest> httpRequest = HttpRequest.POST("/processes/" + processName, request);
        
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(httpRequest, String.class);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
    
    @Test
    public void testCreateProcess_ValidationError_EmptyProcessName() {
        // Given - empty process name
        String processName = "";
        NewProcessRequest request = new NewProcessRequest("valid-id");
        
        // When & Then
        HttpRequest<NewProcessRequest> httpRequest = HttpRequest.POST("/processes/" + processName, request);
        
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(httpRequest, String.class);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
    
    @Test
    public void testCreateProcess_ValidationError_LongProcessName() {
        // Given - process name too long
        String processName = "a".repeat(101); // More than 100 characters
        NewProcessRequest request = new NewProcessRequest("valid-id");
        
        // When & Then
        HttpRequest<NewProcessRequest> httpRequest = HttpRequest.POST("/processes/" + processName, request);
        
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(httpRequest, String.class);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
    
    @Test
    public void testCreateProcess_ValidationError_NegativeDeadline() {
        // Given - negative deadline
        String processName = "deadline-test";
        NewProcessRequest request = new NewProcessRequest("valid-id");
        request.setDeadline(-1L);
        
        // When & Then
        HttpRequest<NewProcessRequest> httpRequest = HttpRequest.POST("/processes/" + processName, request);
        
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(httpRequest, String.class);
        });
        
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}