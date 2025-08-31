package com.totrackit.metrics;

import com.totrackit.service.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Prometheus metrics functionality.
 * Tests that metrics endpoints are available and basic functionality works.
 */
@MicronautTest
public class MetricsIntegrationTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    MetricsService metricsService;

    @Inject
    MeterRegistry meterRegistry;

    @Test
    public void testMetricsEndpointIsAvailable() {
        HttpRequest<String> request = HttpRequest.GET("/metrics");
        HttpResponse<String> response = client.toBlocking().exchange(request, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("# HELP"));
    }

    @Test
    public void testPrometheusEndpointIsAvailable() {
        HttpRequest<String> request = HttpRequest.GET("/prometheus");
        HttpResponse<String> response = client.toBlocking().exchange(request, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        // Should contain some metrics
        assertTrue(response.body().length() > 0);
    }

    @Test
    public void testMeterRegistryIsInjected() {
        assertNotNull(meterRegistry);
        assertNotNull(metricsService);
    }

    @Test
    public void testMetricsServiceCanRecordMetrics() {
        // Test that we can record metrics without errors
        assertDoesNotThrow(() -> {
            metricsService.recordProcessCreated("test-process");
            metricsService.recordDatabaseOperation("create", "processes", true);
            metricsService.recordActiveProcessesCount(5L);
        });
    }

    @Test
    public void testHttpRequestMetrics() {
        // Make an HTTP request to trigger the metrics interceptor
        HttpRequest<String> request = HttpRequest.GET("/health");
        
        // This should not throw an exception
        assertDoesNotThrow(() -> {
            HttpResponse<String> response = client.toBlocking().exchange(request, String.class);
            assertEquals(HttpStatus.OK, response.getStatus());
        });
    }

    @Test
    public void testCustomMetricsCanBeCreated() {
        // Test that we can create custom metrics
        assertDoesNotThrow(() -> {
            metricsService.recordHttpRequest("GET", "/test", 200, 100L);
        });
        
        // Verify the meter registry is working
        assertNotNull(meterRegistry);
        assertTrue(meterRegistry.getMeters().size() > 0);
    }
}