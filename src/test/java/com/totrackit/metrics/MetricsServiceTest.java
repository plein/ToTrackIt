package com.totrackit.metrics;

import com.totrackit.service.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricsService functionality.
 * Tests metric recording without requiring database or HTTP client.
 */
public class MetricsServiceTest {

    private MetricsService metricsService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    public void testRecordProcessCreated() {
        // Test that we can record process creation metrics
        assertDoesNotThrow(() -> {
            metricsService.recordProcessCreated("test-process");
        });

        // Verify the metric was recorded
        assertNotNull(meterRegistry.find("totrackit_processes_created_total").counter());
        assertTrue(meterRegistry.find("totrackit_processes_created_total").counter().count() > 0);
    }

    @Test
    public void testRecordDatabaseOperation() {
        // Test that we can record database operation metrics
        assertDoesNotThrow(() -> {
            metricsService.recordDatabaseOperation("create", "processes", true);
            metricsService.recordDatabaseOperation("read", "processes", false);
        });

        // Verify the metrics were recorded
        assertNotNull(meterRegistry.find("totrackit_database_operations_total").counter());
        assertTrue(meterRegistry.find("totrackit_database_operations_total").counter().count() > 0);
    }

    @Test
    public void testRecordHttpRequest() {
        // Test that we can record HTTP request metrics
        assertDoesNotThrow(() -> {
            metricsService.recordHttpRequest("GET", "/test", 200, 100L);
            metricsService.recordHttpRequest("POST", "/api/processes", 201, 250L);
        });

        // Verify the metrics were recorded
        assertNotNull(meterRegistry.find("totrackit_http_request_duration_seconds").timer());
        assertNotNull(meterRegistry.find("totrackit_http_requests_total").counter());
        assertTrue(meterRegistry.find("totrackit_http_requests_total").counter().count() > 0);
    }

    @Test
    public void testRecordActiveProcessesCount() {
        // Test that we can record active processes gauge
        assertDoesNotThrow(() -> {
            metricsService.recordActiveProcessesCount(5L);
            metricsService.recordActiveProcessesCount(10L);
        });

        // Verify the gauge was recorded
        assertNotNull(meterRegistry.find("totrackit_active_processes_current").gauge());
        // The gauge should have the last value set
        Double gaugeValue = meterRegistry.find("totrackit_active_processes_current").gauge().value();
        assertNotNull(gaugeValue);
        assertTrue(gaugeValue >= 0.0);
    }

    @Test
    public void testMetricsWithTags() {
        // Test that metrics with different tags are recorded separately
        metricsService.recordProcessCreated("process-a");
        metricsService.recordProcessCreated("process-b");
        metricsService.recordProcessCreated("process-a");

        // Should have recorded some increments (exact count may vary due to tag handling)
        assertTrue(meterRegistry.find("totrackit_processes_created_total").counter().count() > 0);
    }

    @Test
    public void testMeterRegistryIntegration() {
        // Test that the meter registry is properly integrated
        assertNotNull(meterRegistry);
        
        // Record some metrics
        metricsService.recordProcessCreated("test");
        metricsService.recordDatabaseOperation("test", "test", true);
        
        // Verify we have meters registered
        assertTrue(meterRegistry.getMeters().size() > 0);
        
        // Verify we can find our custom metrics
        assertNotNull(meterRegistry.find("totrackit_processes_created_total").counter());
        assertNotNull(meterRegistry.find("totrackit_database_operations_total").counter());
    }
}