package com.totrackit.controller;

import com.totrackit.dto.NameRollupEntry;
import com.totrackit.dto.PagedResult;
import com.totrackit.dto.SummaryResponse;
import com.totrackit.dto.TagImpactResponse;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import com.totrackit.repository.ProcessRepository;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the tag-impact analytics endpoint against a real
 * PostgreSQL container (repository writes use PostgreSQL-dialect SQL, so the
 * H2 test datasource cannot seed data). Overdue processes are seeded directly
 * through the repository because the API rejects past deadlines.
 */
@MicronautTest(transactional = false) // seeds must be visible to the server's own connections
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // required for TestPropertyProvider
class AnalyticsControllerIntegrationTest implements TestPropertyProvider {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("totrackit_test")
            .withUsername("test")
            .withPassword("test");

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ProcessRepository processRepository;

    @Override
    @Nonnull
    public Map<String, String> getProperties() {
        // Micronaut resolves properties before the Testcontainers extension
        // runs, so the container must be started here.
        if (!postgres.isRunning()) {
            postgres.start();
        }
        return Map.of(
                "datasources.default.url", postgres.getJdbcUrl(),
                "datasources.default.username", postgres.getUsername(),
                "datasources.default.password", postgres.getPassword(),
                "datasources.default.driver-class-name", postgres.getDriverClassName(),
                // Colima/Docker port forwarding can lag container readiness;
                // let the pool retry instead of failing fast at context start.
                "datasources.default.initialization-fail-timeout", "60000",
                "micronaut.data.default.dialect", "POSTGRES",
                "flyway.datasources.default.enabled", "true"
        );
    }

    private ProcessEntity seed(String name, String id, ProcessStatus status, Instant deadline,
                               Instant completedAt, String tagsJson) {
        ProcessEntity entity = new ProcessEntity(id, name);
        entity.setStatus(status);
        entity.setStartedAt(Instant.now().minusSeconds(3600));
        entity.setDeadline(deadline);
        entity.setCompletedAt(completedAt);
        entity.setTags(tagsJson);
        return processRepository.save(entity);
    }

    @Test
    void testTagImpactBreakdown() {
        String name = "analytics-breakdown-test";
        Instant now = Instant.now();
        seed(name, "overdue-de", ProcessStatus.ACTIVE, now.minusSeconds(600), null,
                "[{\"key\":\"country\",\"value\":\"DE\"}]");
        seed(name, "ontrack-fr", ProcessStatus.ACTIVE, now.plusSeconds(3600), null,
                "[{\"key\":\"country\",\"value\":\"FR\"}]");

        TagImpactResponse response = client.toBlocking().retrieve(
                HttpRequest.GET("/analytics/tags?name=" + name + "&window_hours=24"),
                TagImpactResponse.class);

        assertEquals(2, response.getTotalProcesses());
        assertEquals(1, response.getProblemProcesses());
        assertEquals(2, response.getTags().size());
        assertEquals("DE", response.getTags().get(0).getValue());
        assertEquals(1, response.getTags().get(0).getOverdue());
        assertEquals(1, response.getTags().get(0).getProblems());
        assertEquals("FR", response.getTags().get(1).getValue());
        assertEquals(0, response.getTags().get(1).getProblems());
    }

    @Test
    void testTagImpactFilteredByName() {
        Instant now = Instant.now();
        seed("analytics-filter-test", "overdue-de", ProcessStatus.ACTIVE, now.minusSeconds(600), null,
                "[{\"key\":\"country\",\"value\":\"DE\"}]");

        TagImpactResponse response = client.toBlocking().retrieve(
                HttpRequest.GET("/analytics/tags?name=analytics-filter-no-match"),
                TagImpactResponse.class);

        assertEquals(0, response.getTotalProcesses());
        // Serde's NON_EMPTY inclusion omits the empty array entirely
        assertTrue(response.getTags() == null || response.getTags().isEmpty());
    }

    /** Seeds a completed run with an explicit start so the duration is exact. */
    private void seedCompleted(String name, String id, long durationSeconds, boolean onTime, String tagsJson) {
        Instant completedAt = Instant.now().minusSeconds(60);
        ProcessEntity entity = new ProcessEntity(id, name);
        entity.setStatus(ProcessStatus.COMPLETED);
        entity.setStartedAt(completedAt.minusSeconds(durationSeconds));
        entity.setCompletedAt(completedAt);
        entity.setDeadline(onTime ? completedAt.plusSeconds(600) : completedAt.minusSeconds(600));
        entity.setTags(tagsJson);
        processRepository.save(entity);
    }

    @Test
    void testDurationStatsFromSql() {
        String name = "analytics-duration-test";
        String tags = "[{\"key\":\"country\",\"value\":\"DE\"}]";
        seedCompleted(name, "d100", 100, true, tags);
        seedCompleted(name, "d300", 300, true, tags);

        TagImpactResponse response = client.toBlocking().retrieve(
                HttpRequest.GET("/analytics/tags?name=" + name + "&window_hours=24"),
                TagImpactResponse.class);

        assertEquals(2, response.getTotalProcesses());
        assertEquals(0, response.getProblemProcesses());
        assertNotNull(response.getDuration());
        assertEquals(2, response.getDuration().getCount());
        assertEquals(200.0, response.getDuration().getAvgSeconds(), 0.5);
        // percentile_cont interpolates between ranks; only bound the range
        assertTrue(response.getDuration().getP50Seconds() >= 100 && response.getDuration().getP50Seconds() <= 300);
        assertTrue(response.getDuration().getP99Seconds() <= 300.5);

        assertEquals(1, response.getTags().size());
        assertNotNull(response.getTags().get(0).getDuration());
        assertEquals(2, response.getTags().get(0).getDuration().getCount());
    }

    @Test
    void testWindowExcludesOldFinishedRuns() {
        String name = "analytics-window-test";
        Instant now = Instant.now();
        // Finished 48h ago: outside a 24h window
        ProcessEntity old = new ProcessEntity("old-run", name);
        old.setStatus(ProcessStatus.COMPLETED);
        old.setStartedAt(now.minusSeconds(50 * 3600));
        old.setCompletedAt(now.minusSeconds(48 * 3600));
        processRepository.save(old);
        // Active runs are always in scope, regardless of age
        seed(name, "active-run", ProcessStatus.ACTIVE, now.plusSeconds(3600), null, null);

        TagImpactResponse day = client.toBlocking().retrieve(
                HttpRequest.GET("/analytics/tags?name=" + name + "&window_hours=24"),
                TagImpactResponse.class);
        assertEquals(1, day.getTotalProcesses());

        TagImpactResponse week = client.toBlocking().retrieve(
                HttpRequest.GET("/analytics/tags?name=" + name + "&window_hours=168"),
                TagImpactResponse.class);
        assertEquals(2, week.getTotalProcesses());
    }

    @Test
    void testFailedRunsCountAsProblems() {
        String name = "analytics-failed-test";
        Instant now = Instant.now();
        seed(name, "failed-run", ProcessStatus.FAILED, null, now.minusSeconds(120),
                "[{\"key\":\"provider\",\"value\":\"acme\"}]");

        TagImpactResponse response = client.toBlocking().retrieve(
                HttpRequest.GET("/analytics/tags?name=" + name + "&window_hours=24"),
                TagImpactResponse.class);

        assertEquals(1, response.getTotalProcesses());
        assertEquals(1, response.getProblemProcesses());
        assertEquals(1, response.getTags().get(0).getFailed());
        assertEquals(1, response.getTags().get(0).getProblems());
        // Failed runs contribute no completion-duration stats
        assertTrue(response.getDuration() == null || response.getDuration().getCount() == 0);
    }

    @Test
    void testSummaryEndpoint() {
        String name = "analytics-summary-test";
        Instant now = Instant.now();
        seed(name, "sum-overdue", ProcessStatus.ACTIVE, now.minusSeconds(600), null, null);
        seedCompleted(name, "sum-on-time", 120, true, null);

        SummaryResponse summary = client.toBlocking().retrieve(
                HttpRequest.GET("/analytics/summary"), SummaryResponse.class);

        assertTrue(summary.getTotal() >= 2);
        assertTrue(summary.getActive() >= 1);
        assertTrue(summary.getOverdue() >= 1);
        assertTrue(summary.getCompleted() >= 1);
        assertTrue(summary.getCompletedOnTime() >= 1);
        assertTrue(summary.getCompleted24h() >= 1);
        assertTrue(summary.getGeneratedAt() > 0);
    }

    @Test
    void testNamesRollupEndpoint() {
        String name = "analytics-names-test";
        Instant now = Instant.now();
        seed(name, "nr-overdue", ProcessStatus.ACTIVE, now.minusSeconds(600), null, null);
        seedCompleted(name, "nr-late", 120, false, null);

        PagedResult<NameRollupEntry> result = client.toBlocking().retrieve(
                HttpRequest.GET("/analytics/names?limit=100&offset=0"),
                Argument.of(PagedResult.class, NameRollupEntry.class));

        NameRollupEntry entry = result.getData().stream()
                .filter(e -> name.equals(e.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a rollup entry for " + name));

        assertEquals(2, entry.getTotal());
        assertEquals(1, entry.getActive());
        assertEquals(1, entry.getOverdue());
        assertEquals(1, entry.getCompleted());
        assertEquals(1, entry.getCompletedLate());
        assertEquals(0, entry.getCompletedOnTime());
        assertNotNull(entry.getLastStartedAt());
        assertTrue(result.getTotal() >= 1);
    }
}
