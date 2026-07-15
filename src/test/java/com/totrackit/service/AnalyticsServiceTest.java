package com.totrackit.service;

import com.totrackit.dto.TagImpactEntry;
import com.totrackit.dto.TagImpactResponse;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import com.totrackit.repository.ProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnalyticsService tag-impact aggregation.
 */
@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private ProcessRepository processRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(processRepository);
    }

    private ProcessEntity process(String id, ProcessStatus status, Instant deadline,
                                  Instant completedAt, String tagsJson) {
        ProcessEntity entity = new ProcessEntity(id, "account-activation");
        entity.setStatus(status);
        entity.setStartedAt(Instant.now().minusSeconds(7200));
        entity.setDeadline(deadline);
        entity.setCompletedAt(completedAt);
        entity.setTags(tagsJson);
        return entity;
    }

    private static final String DE_TAGS = "[{\"key\":\"country\",\"value\":\"DE\"}]";
    private static final String FR_TAGS = "[{\"key\":\"country\",\"value\":\"FR\"}]";

    @Test
    void testProblemsConcentratedInOneTag() {
        Instant now = Instant.now();
        List<ProcessEntity> entities = List.of(
                // Two overdue DE activations (active, deadline in the past)
                process("p1", ProcessStatus.ACTIVE, now.minusSeconds(600), null, DE_TAGS),
                process("p2", ProcessStatus.ACTIVE, now.minusSeconds(300), null, DE_TAGS),
                // One late DE completion
                process("p3", ProcessStatus.COMPLETED, now.minusSeconds(900), now.minusSeconds(60), DE_TAGS),
                // Healthy FR traffic: on-track and completed on time
                process("p4", ProcessStatus.ACTIVE, now.plusSeconds(3600), null, FR_TAGS),
                process("p5", ProcessStatus.COMPLETED, now.plusSeconds(3600), now.minusSeconds(60), FR_TAGS)
        );
        when(processRepository.findWithComprehensiveFilters(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(entities);

        TagImpactResponse response = analyticsService.getTagImpact(null, 24);

        assertEquals(5, response.getTotalProcesses());
        assertEquals(3, response.getProblemProcesses());
        assertEquals(2, response.getTags().size());

        // DE first: it owns all the problems
        TagImpactEntry de = response.getTags().get(0);
        assertEquals("country", de.getKey());
        assertEquals("DE", de.getValue());
        assertEquals(2, de.getOverdue());
        assertEquals(1, de.getCompletedLate());
        assertEquals(3, de.getProblems());
        assertEquals(3, de.getTotal());

        TagImpactEntry fr = response.getTags().get(1);
        assertEquals("FR", fr.getValue());
        assertEquals(0, fr.getProblems());
        assertEquals(1, fr.getOnTrack());
        assertEquals(1, fr.getCompletedOnTime());
    }

    @Test
    void testFinishedProcessesOutsideWindowAreExcluded() {
        Instant now = Instant.now();
        List<ProcessEntity> entities = List.of(
                // Completed late, but 3 days ago — outside a 24h window
                process("old", ProcessStatus.COMPLETED, now.minusSeconds(300000),
                        now.minusSeconds(259200), DE_TAGS),
                // Overdue right now — always included regardless of age
                process("stuck", ProcessStatus.ACTIVE, now.minusSeconds(600), null, DE_TAGS)
        );
        when(processRepository.findWithComprehensiveFilters(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(entities);

        TagImpactResponse response = analyticsService.getTagImpact(null, 24);

        assertEquals(1, response.getTotalProcesses());
        assertEquals(1, response.getProblemProcesses());
        TagImpactEntry de = response.getTags().get(0);
        assertEquals(1, de.getOverdue());
        assertEquals(0, de.getCompletedLate());
    }

    @Test
    void testFailedProcessesCountAsProblems() {
        Instant now = Instant.now();
        List<ProcessEntity> entities = List.of(
                process("f1", ProcessStatus.FAILED, null, now.minusSeconds(60), DE_TAGS)
        );
        when(processRepository.findWithComprehensiveFilters(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(entities);

        TagImpactResponse response = analyticsService.getTagImpact(null, 24);

        assertEquals(1, response.getProblemProcesses());
        assertEquals(1, response.getTags().get(0).getFailed());
        assertEquals(1, response.getTags().get(0).getProblems());
    }

    @Test
    void testUntaggedAndMalformedTagsAreCountedInTotalsOnly() {
        Instant now = Instant.now();
        List<ProcessEntity> entities = List.of(
                process("u1", ProcessStatus.ACTIVE, now.minusSeconds(600), null, null),
                process("u2", ProcessStatus.ACTIVE, now.minusSeconds(600), null, "not-json")
        );
        when(processRepository.findWithComprehensiveFilters(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(entities);

        TagImpactResponse response = analyticsService.getTagImpact(null, 24);

        assertEquals(2, response.getTotalProcesses());
        assertEquals(2, response.getProblemProcesses());
        assertTrue(response.getTags().isEmpty());
    }

    @Test
    void testDurationStatsOverallAndPerTag() {
        Instant now = Instant.now();
        // Three DE completions taking 100s, 200s, 300s; one FR completion taking 10s
        ProcessEntity de1 = process("d1", ProcessStatus.COMPLETED, now.plusSeconds(3600), now.minusSeconds(60), DE_TAGS);
        de1.setStartedAt(de1.getCompletedAt().minusSeconds(100));
        ProcessEntity de2 = process("d2", ProcessStatus.COMPLETED, now.plusSeconds(3600), now.minusSeconds(60), DE_TAGS);
        de2.setStartedAt(de2.getCompletedAt().minusSeconds(200));
        ProcessEntity de3 = process("d3", ProcessStatus.COMPLETED, now.plusSeconds(3600), now.minusSeconds(60), DE_TAGS);
        de3.setStartedAt(de3.getCompletedAt().minusSeconds(300));
        ProcessEntity fr1 = process("f1", ProcessStatus.COMPLETED, now.plusSeconds(3600), now.minusSeconds(60), FR_TAGS);
        fr1.setStartedAt(fr1.getCompletedAt().minusSeconds(10));
        // Active + failed runs must not contribute durations
        ProcessEntity active = process("a1", ProcessStatus.ACTIVE, now.plusSeconds(3600), null, DE_TAGS);
        ProcessEntity failed = process("x1", ProcessStatus.FAILED, null, now.minusSeconds(30), DE_TAGS);

        when(processRepository.findWithComprehensiveFilters(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of(de1, de2, de3, fr1, active, failed));

        TagImpactResponse response = analyticsService.getTagImpact(null, 24);

        assertEquals(4, response.getDuration().getCount());
        assertEquals(152.5, response.getDuration().getAvgSeconds());
        assertEquals(100.0, response.getDuration().getP50Seconds());
        assertEquals(300.0, response.getDuration().getP90Seconds());

        TagImpactEntry de = response.getTags().stream()
                .filter(t -> "DE".equals(t.getValue())).findFirst().orElseThrow();
        assertEquals(3, de.getDuration().getCount());
        assertEquals(200.0, de.getDuration().getAvgSeconds());
        assertEquals(300.0, de.getDuration().getP90Seconds());

        TagImpactEntry fr = response.getTags().stream()
                .filter(t -> "FR".equals(t.getValue())).findFirst().orElseThrow();
        assertEquals(1, fr.getDuration().getCount());
        assertEquals(10.0, fr.getDuration().getP90Seconds());
    }

    @Test
    void testNoDurationStatsWhenNothingCompleted() {
        Instant now = Instant.now();
        when(processRepository.findWithComprehensiveFilters(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of(process("a1", ProcessStatus.ACTIVE, now.plusSeconds(60), null, DE_TAGS)));

        TagImpactResponse response = analyticsService.getTagImpact(null, 24);

        assertNull(response.getDuration());
        assertNull(response.getTags().get(0).getDuration());
    }

    @Test
    void testProcessesWithoutDeadlineAreOnTrackNotProblems() {
        List<ProcessEntity> entities = List.of(
                process("nd", ProcessStatus.ACTIVE, null, null, DE_TAGS)
        );
        when(processRepository.findWithComprehensiveFilters(isNull(), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(entities);

        TagImpactResponse response = analyticsService.getTagImpact(null, 24);

        assertEquals(0, response.getProblemProcesses());
        assertEquals(1, response.getTags().get(0).getOnTrack());
    }
}
