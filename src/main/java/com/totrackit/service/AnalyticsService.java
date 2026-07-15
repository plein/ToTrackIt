package com.totrackit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.totrackit.dto.DurationStats;
import com.totrackit.dto.TagImpactEntry;
import com.totrackit.dto.TagImpactResponse;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.ProcessStatus;
import com.totrackit.model.ProcessTag;
import com.totrackit.repository.ProcessRepository;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated statistics across processes, keyed by tag. Answers "where are the
 * problems concentrated?" — e.g. all overdue activations share country=DE.
 *
 * Aggregation happens in memory over the filtered process list, consistent
 * with the filtering approach in {@link ProcessService} and portable across
 * PostgreSQL and the H2 test schema.
 */
@Singleton
public class AnalyticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);
    private static final int MAX_TAG_ROWS = 100;

    private final ProcessRepository processRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public AnalyticsService(ProcessRepository processRepository) {
        this.processRepository = processRepository;
    }

    /**
     * Computes per-tag deadline outcomes.
     *
     * Active processes are always included (an overdue process is a current
     * problem no matter when it started); finished processes are included when
     * they finished within the window.
     *
     * @param name        optional process name filter
     * @param windowHours how far back to include finished processes
     * @return per-tag impact breakdown, most problematic tags first
     */
    public TagImpactResponse getTagImpact(@Nullable String name, int windowHours) {
        Instant now = Instant.now();
        Instant since = now.minus(Duration.ofHours(windowHours));

        List<ProcessEntity> entities = processRepository.findWithComprehensiveFilters(
                name, null, null, Integer.MAX_VALUE, 0);

        Map<String, TagImpactEntry> byTag = new LinkedHashMap<>();
        Map<String, List<Long>> durationsByTag = new LinkedHashMap<>();
        List<Long> allDurations = new ArrayList<>();
        long total = 0;
        long problems = 0;

        for (ProcessEntity entity : entities) {
            Outcome outcome = classify(entity, now, since);
            if (outcome == Outcome.OUT_OF_WINDOW) {
                continue;
            }
            total++;
            if (outcome.isProblem()) {
                problems++;
            }
            Long duration = completionDuration(entity, outcome);
            if (duration != null) {
                allDurations.add(duration);
            }
            for (ProcessTag tag : parseTags(entity.getTags())) {
                TagImpactEntry entry = byTag.computeIfAbsent(tag.getKey() + '\0' + tag.getValue(),
                        k -> new TagImpactEntry(tag.getKey(), tag.getValue()));
                entry.setTotal(entry.getTotal() + 1);
                switch (outcome) {
                    case OVERDUE -> entry.setOverdue(entry.getOverdue() + 1);
                    case ON_TRACK -> entry.setOnTrack(entry.getOnTrack() + 1);
                    case COMPLETED_LATE -> entry.setCompletedLate(entry.getCompletedLate() + 1);
                    case COMPLETED_ON_TIME -> entry.setCompletedOnTime(entry.getCompletedOnTime() + 1);
                    case FAILED -> entry.setFailed(entry.getFailed() + 1);
                    default -> { }
                }
                entry.setProblems(entry.getOverdue() + entry.getCompletedLate() + entry.getFailed());
                if (duration != null) {
                    durationsByTag.computeIfAbsent(tag.getKey() + '\0' + tag.getValue(),
                            k -> new ArrayList<>()).add(duration);
                }
            }
        }

        byTag.forEach((key, entry) -> entry.setDuration(DurationStats.of(durationsByTag.get(key))));

        List<TagImpactEntry> rows = byTag.values().stream()
                .sorted(Comparator.comparingLong(TagImpactEntry::getProblems).reversed()
                        .thenComparing(Comparator.comparingLong(TagImpactEntry::getTotal).reversed())
                        .thenComparing(TagImpactEntry::getKey)
                        .thenComparing(TagImpactEntry::getValue))
                .limit(MAX_TAG_ROWS)
                .toList();

        TagImpactResponse response = new TagImpactResponse();
        response.setWindowHours(windowHours);
        response.setGeneratedAt(now.getEpochSecond());
        response.setTotalProcesses(total);
        response.setProblemProcesses(problems);
        response.setDuration(DurationStats.of(allDurations));
        response.setTags(rows);
        return response;
    }

    /**
     * Duration in seconds for finished-successfully runs (on time or late);
     * null for active/failed runs or when timestamps are missing.
     */
    private Long completionDuration(ProcessEntity entity, Outcome outcome) {
        if (outcome != Outcome.COMPLETED_ON_TIME && outcome != Outcome.COMPLETED_LATE) {
            return null;
        }
        if (entity.getStartedAt() == null || entity.getCompletedAt() == null) {
            return null;
        }
        return entity.getCompletedAt().getEpochSecond() - entity.getStartedAt().getEpochSecond();
    }

    private enum Outcome {
        OVERDUE, ON_TRACK, COMPLETED_LATE, COMPLETED_ON_TIME, FAILED, OUT_OF_WINDOW;

        boolean isProblem() {
            return this == OVERDUE || this == COMPLETED_LATE || this == FAILED;
        }
    }

    private Outcome classify(ProcessEntity entity, Instant now, Instant since) {
        if (entity.getStatus() == ProcessStatus.ACTIVE) {
            boolean overdue = entity.getDeadline() != null && now.isAfter(entity.getDeadline());
            return overdue ? Outcome.OVERDUE : Outcome.ON_TRACK;
        }
        Instant finishedAt = entity.getCompletedAt();
        if (finishedAt == null || finishedAt.isBefore(since)) {
            return Outcome.OUT_OF_WINDOW;
        }
        if (entity.getStatus() == ProcessStatus.FAILED) {
            return Outcome.FAILED;
        }
        boolean late = entity.getDeadline() != null && finishedAt.isAfter(entity.getDeadline());
        return late ? Outcome.COMPLETED_LATE : Outcome.COMPLETED_ON_TIME;
    }

    private List<ProcessTag> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<List<ProcessTag>>() {});
        } catch (Exception e) {
            LOG.warn("Failed to parse tags JSON during analytics aggregation: {}", tagsJson);
            return List.of();
        }
    }
}
