package com.totrackit.controller;

import com.totrackit.dto.TagImpactResponse;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import com.totrackit.repository.ProcessRepository;
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
}
