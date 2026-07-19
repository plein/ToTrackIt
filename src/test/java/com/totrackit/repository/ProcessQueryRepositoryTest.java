package com.totrackit.repository;

import com.totrackit.dto.Pageable;
import com.totrackit.dto.ProcessFilter;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.DeadlineStatus;
import com.totrackit.model.ProcessStatus;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PostgreSQL-backed tests for the dynamic list/count query path (JSONB tag
 * containment, deadline-status translation, sorting, pagination) and the
 * SQL-side warning threshold scan. These queries are PostgreSQL-native and
 * cannot run on H2.
 */
@MicronautTest(transactional = false)
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProcessQueryRepositoryTest implements TestPropertyProvider {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("totrackit_test")
            .withUsername("test")
            .withPassword("test");

    @Inject
    ProcessQueryRepository queryRepository;

    @Inject
    ProcessRepository processRepository;

    @Override
    @Nonnull
    public Map<String, String> getProperties() {
        if (!postgres.isRunning()) {
            postgres.start();
        }
        return Map.of(
                "datasources.default.url", postgres.getJdbcUrl(),
                "datasources.default.username", postgres.getUsername(),
                "datasources.default.password", postgres.getPassword(),
                "datasources.default.driver-class-name", postgres.getDriverClassName(),
                "datasources.default.initialization-fail-timeout", "60000",
                "micronaut.data.default.dialect", "POSTGRES",
                "flyway.datasources.default.enabled", "true"
        );
    }

    private ProcessEntity seed(String name, String id, ProcessStatus status, Instant startedAt,
                               Instant deadline, Instant completedAt, String tagsJson) {
        ProcessEntity entity = new ProcessEntity(id, name);
        entity.setStatus(status);
        entity.setStartedAt(startedAt);
        entity.setDeadline(deadline);
        entity.setCompletedAt(completedAt);
        entity.setTags(tagsJson);
        return processRepository.save(entity);
    }

    private ProcessFilter filterForName(String name) {
        ProcessFilter filter = new ProcessFilter();
        filter.setName(name);
        return filter;
    }

    private List<String> ids(List<ProcessEntity> entities) {
        return entities.stream().map(ProcessEntity::getProcessId).toList();
    }

    @Test
    void testStatusFilterAndCount() {
        String name = "qr-status-test";
        Instant now = Instant.now();
        seed(name, "a1", ProcessStatus.ACTIVE, now.minusSeconds(100), null, null, null);
        seed(name, "c1", ProcessStatus.COMPLETED, now.minusSeconds(200), null, now.minusSeconds(50), null);
        seed(name, "f1", ProcessStatus.FAILED, now.minusSeconds(300), null, now.minusSeconds(60), null);

        ProcessFilter filter = filterForName(name);
        filter.setStatus(ProcessStatus.ACTIVE);

        List<ProcessEntity> page = queryRepository.findPage(filter, new Pageable());
        assertEquals(List.of("a1"), ids(page));
        assertEquals(1, queryRepository.count(filter));

        assertEquals(3, queryRepository.count(filterForName(name)));
    }

    @Test
    void testMultiTagContainmentIsAndComposed() {
        String name = "qr-tags-test";
        Instant now = Instant.now();
        seed(name, "de-web", ProcessStatus.ACTIVE, now.minusSeconds(10), null, null,
                "[{\"key\":\"country\",\"value\":\"DE\"},{\"key\":\"channel\",\"value\":\"web\"}]");
        seed(name, "de-app", ProcessStatus.ACTIVE, now.minusSeconds(20), null, null,
                "[{\"key\":\"country\",\"value\":\"DE\"},{\"key\":\"channel\",\"value\":\"app\"}]");
        seed(name, "fr-web", ProcessStatus.ACTIVE, now.minusSeconds(30), null, null,
                "[{\"key\":\"country\",\"value\":\"FR\"},{\"key\":\"channel\",\"value\":\"web\"}]");
        seed(name, "untagged", ProcessStatus.ACTIVE, now.minusSeconds(40), null, null, null);

        ProcessFilter oneTag = filterForName(name);
        oneTag.setTags(Map.of("country", "DE"));
        assertEquals(2, queryRepository.count(oneTag));

        ProcessFilter twoTags = filterForName(name);
        twoTags.setTags(new java.util.LinkedHashMap<>(Map.of("country", "DE", "channel", "web")));
        List<ProcessEntity> page = queryRepository.findPage(twoTags, new Pageable());
        assertEquals(List.of("de-web"), ids(page));

        // Value must match the same tag object, not just any key/value anywhere
        ProcessFilter crossed = filterForName(name);
        crossed.setTags(Map.of("country", "web"));
        assertEquals(0, queryRepository.count(crossed));
    }

    @Test
    void testDeadlineStatusTranslation() {
        String name = "qr-deadline-status-test";
        Instant now = Instant.now();
        seed(name, "missed", ProcessStatus.ACTIVE, now.minusSeconds(7200), now.minusSeconds(3600), null, null);
        seed(name, "on-track", ProcessStatus.ACTIVE, now.minusSeconds(60), now.plusSeconds(3600), null, null);
        seed(name, "on-time", ProcessStatus.COMPLETED, now.minusSeconds(7200), now.minusSeconds(3600),
                now.minusSeconds(5400), null);
        seed(name, "late", ProcessStatus.COMPLETED, now.minusSeconds(7200), now.minusSeconds(3600),
                now.minusSeconds(600), null);
        seed(name, "no-deadline", ProcessStatus.ACTIVE, now.minusSeconds(60), null, null, null);

        for (var expected : Map.of(
                DeadlineStatus.MISSED, "missed",
                DeadlineStatus.ON_TRACK, "on-track",
                DeadlineStatus.COMPLETED_ON_TIME, "on-time",
                DeadlineStatus.COMPLETED_LATE, "late").entrySet()) {
            ProcessFilter filter = filterForName(name);
            filter.setDeadlineStatus(expected.getKey());
            assertEquals(List.of(expected.getValue()), ids(queryRepository.findPage(filter, new Pageable())),
                    "deadline_status=" + expected.getKey());
        }
    }

    @Test
    void testDeadlineRangeFilters() {
        String name = "qr-deadline-range-test";
        Instant now = Instant.now();
        seed(name, "soon", ProcessStatus.ACTIVE, now, now.plusSeconds(600), null, null);
        seed(name, "later", ProcessStatus.ACTIVE, now, now.plusSeconds(7200), null, null);
        seed(name, "none", ProcessStatus.ACTIVE, now, null, null, null);

        ProcessFilter before = filterForName(name);
        before.setDeadlineBefore(now.plusSeconds(3600).getEpochSecond());
        assertEquals(List.of("soon"), ids(queryRepository.findPage(before, new Pageable())));

        ProcessFilter after = filterForName(name);
        after.setDeadlineAfter(now.plusSeconds(3600).getEpochSecond());
        assertEquals(List.of("later"), ids(queryRepository.findPage(after, new Pageable())));
    }

    @Test
    void testRunningDurationFilter() {
        String name = "qr-running-duration-test";
        Instant now = Instant.now();
        seed(name, "long-runner", ProcessStatus.ACTIVE, now.minusSeconds(7200), null, null, null);
        seed(name, "short-runner", ProcessStatus.ACTIVE, now.minusSeconds(600), null, null, null);
        seed(name, "old-completed", ProcessStatus.COMPLETED, now.minusSeconds(7200), null,
                now.minusSeconds(300), null);

        ProcessFilter filter = filterForName(name);
        filter.setRunningDurationMin(60); // minutes
        assertEquals(List.of("long-runner"), ids(queryRepository.findPage(filter, new Pageable())));
    }

    @Test
    void testSortingAndNullsLast() {
        String name = "qr-sorting-test";
        Instant now = Instant.now();
        seed(name, "b", ProcessStatus.ACTIVE, now.minusSeconds(30), now.plusSeconds(600), null, null);
        seed(name, "a", ProcessStatus.ACTIVE, now.minusSeconds(20), now.plusSeconds(60), null, null);
        seed(name, "c", ProcessStatus.ACTIVE, now.minusSeconds(10), null, null, null);

        ProcessFilter byDeadlineAsc = filterForName(name);
        byDeadlineAsc.setSortBy("deadline");
        byDeadlineAsc.setSortDirection("asc");
        assertEquals(List.of("a", "b", "c"), ids(queryRepository.findPage(byDeadlineAsc, new Pageable())));

        ProcessFilter byDeadlineDesc = filterForName(name);
        byDeadlineDesc.setSortBy("deadline");
        byDeadlineDesc.setSortDirection("desc");
        // NULLS LAST in both directions
        assertEquals(List.of("b", "a", "c"), ids(queryRepository.findPage(byDeadlineDesc, new Pageable())));

        ProcessFilter defaultSort = filterForName(name);
        assertEquals(List.of("c", "a", "b"), ids(queryRepository.findPage(defaultSort, new Pageable())));

        // Unknown sort keys fall back to started_at instead of being interpolated
        ProcessFilter unknown = filterForName(name);
        unknown.setSortBy("evil; DROP TABLE processes;--");
        assertEquals(3, queryRepository.findPage(unknown, new Pageable()).size());
    }

    @Test
    void testPagination() {
        String name = "qr-pagination-test";
        Instant now = Instant.now();
        for (int i = 0; i < 5; i++) {
            seed(name, "run-" + i, ProcessStatus.ACTIVE, now.minusSeconds(100 - i), null, null, null);
        }

        ProcessFilter filter = filterForName(name);
        List<ProcessEntity> firstPage = queryRepository.findPage(filter, new Pageable(2, 0));
        List<ProcessEntity> secondPage = queryRepository.findPage(filter, new Pageable(2, 2));

        assertEquals(List.of("run-4", "run-3"), ids(firstPage));
        assertEquals(List.of("run-2", "run-1"), ids(secondPage));
        assertEquals(5, queryRepository.count(filter));
    }

    @Test
    void testCountOverdueByName() {
        Instant now = Instant.now();
        seed("qr-overdue-a", "o1", ProcessStatus.ACTIVE, now.minusSeconds(7200), now.minusSeconds(3600), null, null);
        seed("qr-overdue-a", "o2", ProcessStatus.ACTIVE, now.minusSeconds(7200), now.minusSeconds(1800), null, null);
        seed("qr-overdue-b", "o3", ProcessStatus.ACTIVE, now.minusSeconds(7200), now.minusSeconds(600), null, null);
        seed("qr-overdue-b", "fine", ProcessStatus.ACTIVE, now.minusSeconds(60), now.plusSeconds(3600), null, null);

        Map<String, Long> counts = queryRepository.countOverdueByName(Instant.now(), 100);
        assertEquals(2L, counts.get("qr-overdue-a"));
        assertEquals(1L, counts.get("qr-overdue-b"));
    }

    @Test
    void testWarningScanThresholdBoundaries() {
        String name = "qr-warning-boundary-test";
        Instant now = Instant.now();
        // Exactly at the 0.75 threshold of a 1000s budget: included
        seed(name, "at-threshold", ProcessStatus.ACTIVE, now.minusSeconds(750), now.plusSeconds(250), null, null);
        // Below threshold: excluded
        seed(name, "early", ProcessStatus.ACTIVE, now.minusSeconds(200), now.plusSeconds(800), null, null);
        // Zero/negative budget (deadline before start): excluded
        seed(name, "zero-budget", ProcessStatus.ACTIVE, now.plusSeconds(100), now.minusSeconds(5), null, null);
        // Already past deadline: handled by the missed pass, not the warning pass
        seed(name, "past-deadline", ProcessStatus.ACTIVE, now.minusSeconds(7200), now.minusSeconds(3600), null, null);

        List<String> due = processRepository.findApproachingUnwarned(Instant.now(), 0.75, 100).stream()
                .filter(p -> name.equals(p.getName()))
                .map(ProcessEntity::getProcessId)
                .toList();

        assertEquals(List.of("at-threshold"), due);
    }

    @Test
    void testWarningScanRespectsBatchLimit() {
        String name = "qr-warning-batch-test";
        Instant now = Instant.now();
        for (int i = 0; i < 4; i++) {
            seed(name, "w-" + i, ProcessStatus.ACTIVE, now.minusSeconds(900), now.plusSeconds(100 + i), null, null);
        }

        List<ProcessEntity> batch = processRepository.findApproachingUnwarned(Instant.now(), 0.75, 2);
        assertTrue(batch.size() <= 2);
    }
}
