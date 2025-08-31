package com.totrackit.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.annotation.Nonnull;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for HealthController with real database
 */
@MicronautTest
@Testcontainers
class HealthControllerIntegrationTest implements TestPropertyProvider {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("totrackit_test")
            .withUsername("test")
            .withPassword("test");

    @Inject
    @Client("/")
    HttpClient client;

    @Override
    @Nonnull
    public Map<String, String> getProperties() {
        return Map.of(
                "datasources.default.url", postgres.getJdbcUrl(),
                "datasources.default.username", postgres.getUsername(),
                "datasources.default.password", postgres.getPassword(),
                "datasources.default.driver-class-name", postgres.getDriverClassName()
        );
    }

    @Test
    @DisplayName("Health endpoint should work with real database")
    void testHealthEndpointWithDatabase() {
        HttpRequest<Object> request = HttpRequest.GET("/health");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        
        Map<String, Object> body = response.body();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        
        // Check database component
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) body.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) components.get("database");
        
        assertNotNull(database);
        assertEquals("UP", database.get("status"));
        assertTrue(database.containsKey("responseTime"));
        
        // Response time should be reasonable (less than 1 second)
        String responseTime = (String) database.get("responseTime");
        assertTrue(responseTime.endsWith("ms"));
        int timeMs = Integer.parseInt(responseTime.replace("ms", ""));
        assertTrue(timeMs < 1000, "Database response time should be less than 1 second");
    }

    @Test
    @DisplayName("Ready endpoint should verify database connectivity and migrations")
    void testReadyEndpointWithDatabase() {
        HttpRequest<Object> request = HttpRequest.GET("/health/ready");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        
        Map<String, Object> body = response.body();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        
        // Check database connectivity
        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) body.get("database");
        assertNotNull(database);
        assertEquals("UP", database.get("status"));
        
        // Check migration status
        @SuppressWarnings("unchecked")
        Map<String, Object> migrations = (Map<String, Object>) body.get("migrations");
        assertNotNull(migrations);
        assertEquals("UP", migrations.get("status"));
        assertTrue(migrations.containsKey("latestVersion"));
        assertTrue(migrations.containsKey("success"));
        assertEquals(true, migrations.get("success"));
    }

    @Test
    @DisplayName("Live endpoint should work independently of database")
    void testLiveEndpointIndependentOfDatabase() {
        HttpRequest<Object> request = HttpRequest.GET("/health/live");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        
        Map<String, Object> body = response.body();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        
        // Check application health
        @SuppressWarnings("unchecked")
        Map<String, Object> application = (Map<String, Object>) body.get("application");
        assertNotNull(application);
        assertEquals("UP", application.get("status"));
        assertTrue(application.containsKey("uptime"));
        assertTrue(application.containsKey("memoryUsage"));
        assertTrue(application.containsKey("availableProcessors"));
    }

    @Test
    @DisplayName("Health endpoint should include connection pool information")
    void testHealthEndpointConnectionPoolInfo() {
        HttpRequest<Object> request = HttpRequest.GET("/health");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        Map<String, Object> body = response.body();
        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) body.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> database = (Map<String, Object>) components.get("database");
        
        // Connection pool info might be available
        if (database.containsKey("connectionPool")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> poolInfo = (Map<String, Object>) database.get("connectionPool");
            assertNotNull(poolInfo);
            
            // If pool info is available, it should have meaningful data
            if (!poolInfo.isEmpty()) {
                assertTrue(poolInfo.containsKey("activeConnections") || 
                          poolInfo.containsKey("totalConnections"));
            }
        }
    }

    @Test
    @DisplayName("Migration status should include version information")
    void testMigrationStatusDetails() {
        HttpRequest<Object> request = HttpRequest.GET("/health/ready");
        HttpResponse<Map> response = client.toBlocking().exchange(request, Map.class);

        Map<String, Object> body = response.body();
        @SuppressWarnings("unchecked")
        Map<String, Object> migrations = (Map<String, Object>) body.get("migrations");
        
        assertNotNull(migrations);
        assertEquals("UP", migrations.get("status"));
        
        // Should have version information
        assertTrue(migrations.containsKey("latestVersion"));
        assertTrue(migrations.containsKey("latestDescription"));
        assertTrue(migrations.containsKey("success"));
        
        // Version should be a string
        assertNotNull(migrations.get("latestVersion"));
        assertTrue(migrations.get("latestVersion") instanceof String);
        
        // Success should be true for a working migration
        assertEquals(true, migrations.get("success"));
    }
}