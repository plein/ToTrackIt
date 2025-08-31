package com.totrackit.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HealthController endpoints
 */
@MicronautTest
class HealthControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    @DisplayName("Health endpoint should return application status")
    void testHealthEndpoint() {
        HttpRequest<Object> request = HttpRequest.GET("/health");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        
        Map<String, Object> body = response.body();
        assertNotNull(body);
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("version"));
        assertTrue(body.containsKey("components"));
        
        // Check components
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) body.get("components");
        assertTrue(components.containsKey("database"));
        assertTrue(components.containsKey("application"));
        
        // Status should be UP for healthy application
        assertEquals("UP", body.get("status"));
    }

    @Test
    @DisplayName("Ready endpoint should return readiness status")
    void testReadyEndpoint() {
        HttpRequest<Object> request = HttpRequest.GET("/health/ready");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        
        Map<String, Object> body = response.body();
        assertNotNull(body);
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("database"));
        assertTrue(body.containsKey("migrations"));
        
        // Status should be UP for ready application
        assertEquals("UP", body.get("status"));
    }

    @Test
    @DisplayName("Live endpoint should return liveness status")
    void testLiveEndpoint() {
        HttpRequest<Object> request = HttpRequest.GET("/health/live");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        
        Map<String, Object> body = response.body();
        assertNotNull(body);
        assertTrue(body.containsKey("status"));
        assertTrue(body.containsKey("timestamp"));
        assertTrue(body.containsKey("application"));
        
        // Status should be UP for live application
        assertEquals("UP", body.get("status"));
    }

    @Test
    @DisplayName("Health endpoint should include database health details")
    void testHealthEndpointDatabaseDetails() {
        HttpRequest<Object> request = HttpRequest.GET("/health");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        Map<String, Object> body = response.body();
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) body.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) components.get("database");
        
        assertNotNull(database);
        assertTrue(database.containsKey("status"));
        
        // If database is UP, should have response time
        if ("UP".equals(database.get("status"))) {
            assertTrue(database.containsKey("responseTime"));
        }
    }

    @Test
    @DisplayName("Health endpoint should include application health details")
    void testHealthEndpointApplicationDetails() {
        HttpRequest<Object> request = HttpRequest.GET("/health");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        Map<String, Object> body = response.body();
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) body.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> application = (Map<String, Object>) components.get("application");
        
        assertNotNull(application);
        assertTrue(application.containsKey("status"));
        
        // If application is UP, should have runtime details
        if ("UP".equals(application.get("status"))) {
            assertTrue(application.containsKey("uptime"));
            assertTrue(application.containsKey("memoryUsage"));
            assertTrue(application.containsKey("availableProcessors"));
        }
    }

    @Test
    @DisplayName("Ready endpoint should include migration status")
    void testReadyEndpointMigrationStatus() {
        HttpRequest<Object> request = HttpRequest.GET("/health/ready");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        Map<String, Object> body = response.body();
        @SuppressWarnings("unchecked")
        Map<String, Object> migrations = (Map<String, Object>) body.get("migrations");
        
        assertNotNull(migrations);
        assertTrue(migrations.containsKey("status"));
        
        // If migrations are UP, should have version info
        if ("UP".equals(migrations.get("status"))) {
            assertTrue(migrations.containsKey("latestVersion"));
            assertTrue(migrations.containsKey("success"));
        }
    }
}