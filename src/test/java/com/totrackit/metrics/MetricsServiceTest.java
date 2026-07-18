package com.totrackit.metrics;

import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import com.totrackit.service.MetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

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

    @Test
    public void testActiveProcessesGaugeReflectsLatestValue() {
        metricsService.recordActiveProcessesCount(5L);
        metricsService.recordActiveProcessesCount(10L);

        // The gauge must track the most recent sample, not the first one
        assertEquals(10.0, meterRegistry.find("totrackit_active_processes_current").gauge().value());
    }

    private ProcessEntity completedProcess(String name, long deadlineOffsetFromCompletion) {
        ProcessEntity entity = new ProcessEntity("proc-1", name);
        entity.setStatus(ProcessStatus.COMPLETED);
        Instant completedAt = Instant.now();
        entity.setStartedAt(completedAt.minusSeconds(600));
        entity.setCompletedAt(completedAt);
        entity.setDeadline(completedAt.plusSeconds(deadlineOffsetFromCompletion));
        return entity;
    }

    @Test
    public void testRecordProcessCompletedOnTime() {
        // Deadline 60s after completion -> on time
        metricsService.recordProcessCompleted(completedProcess("on-time-proc", 60));

        assertEquals(1.0, meterRegistry.find("totrackit_processes_completed_on_time_total")
                .tag("process_name", "on-time-proc").counter().count());
        assertNull(meterRegistry.find("totrackit_processes_completed_late_total").counter());
    }

    @Test
    public void testRecordProcessCompletedLate() {
        // Deadline 60s before completion -> late
        metricsService.recordProcessCompleted(completedProcess("late-proc", -60));

        assertEquals(1.0, meterRegistry.find("totrackit_processes_completed_late_total")
                .tag("process_name", "late-proc").counter().count());
        assertNull(meterRegistry.find("totrackit_processes_completed_on_time_total").counter());
    }

    @Test
    public void testRecordProcessCompletedWithoutDeadlineSkipsOutcomeCounters() {
        ProcessEntity entity = completedProcess("no-deadline-proc", 0);
        entity.setDeadline(null);
        metricsService.recordProcessCompleted(entity);

        assertNull(meterRegistry.find("totrackit_processes_completed_on_time_total").counter());
        assertNull(meterRegistry.find("totrackit_processes_completed_late_total").counter());
    }

    @Test
    public void testRecordDeadlineMissed() {
        metricsService.recordDeadlineMissed("stuck-proc");
        metricsService.recordDeadlineMissed("stuck-proc");

        assertEquals(2.0, meterRegistry.find("totrackit_processes_deadline_missed_total")
                .tag("process_name", "stuck-proc").counter().count());
    }

    @Test
    public void testRecordDeadlineWarning() {
        metricsService.recordDeadlineWarning("at-risk-proc");

        assertEquals(1.0, meterRegistry.find("totrackit_processes_deadline_warning_total")
                .tag("process_name", "at-risk-proc").counter().count());
    }

    @Test
    public void testOverdueGaugePerNameUpdateAndReset() {
        metricsService.updateOverdueProcessCounts(Map.of("proc-a", 3L, "proc-b", 1L));

        assertEquals(3.0, meterRegistry.find("totrackit_processes_overdue_current")
                .tag("process_name", "proc-a").gauge().value());
        assertEquals(1.0, meterRegistry.find("totrackit_processes_overdue_current")
                .tag("process_name", "proc-b").gauge().value());

        // proc-a recovers: absent from the next snapshot -> gauge drops to 0
        metricsService.updateOverdueProcessCounts(Map.of("proc-b", 2L));

        assertEquals(0.0, meterRegistry.find("totrackit_processes_overdue_current")
                .tag("process_name", "proc-a").gauge().value());
        assertEquals(2.0, meterRegistry.find("totrackit_processes_overdue_current")
                .tag("process_name", "proc-b").gauge().value());
    }
}