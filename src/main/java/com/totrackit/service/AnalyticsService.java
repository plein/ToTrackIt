package com.totrackit.service;

import com.totrackit.dto.DurationStats;
import com.totrackit.dto.NameRollupEntry;
import com.totrackit.dto.PagedResult;
import com.totrackit.dto.SummaryResponse;
import com.totrackit.dto.TagImpactEntry;
import com.totrackit.dto.TagImpactResponse;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated statistics across processes, keyed by tag. Answers "where are the
 * problems concentrated?" — e.g. all overdue activations share country=DE.
 *
 * Aggregation happens in PostgreSQL: outcomes are classified in a scoped CTE
 * over the stored timestamps (mirroring the read-time DeadlineStatus model),
 * tags are unnested with jsonb_array_elements, and counts/percentiles come
 * back as one row per tag instead of the whole table.
 */
@Singleton
public class AnalyticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);
    private static final int MAX_TAG_ROWS = 100;

    /**
     * Classifies every in-window process exactly like the former Java
     * classify(): ACTIVE rows are always in-window (an overdue process is a
     * current problem no matter when it started); finished rows count when
     * they finished within the window. duration_s is only present for
     * successfully completed runs with both timestamps.
     */
    private static final String SCOPED_CTE_PREFIX =
            "WITH scoped AS (" +
            "  SELECT tags," +
            "    CASE" +
            "      WHEN status = 'ACTIVE' AND deadline IS NOT NULL AND deadline < ? THEN 'OVERDUE'" +
            "      WHEN status = 'ACTIVE' THEN 'ON_TRACK'" +
            "      WHEN status = 'FAILED' THEN 'FAILED'" +
            "      WHEN deadline IS NOT NULL AND completed_at > deadline THEN 'COMPLETED_LATE'" +
            "      ELSE 'COMPLETED_ON_TIME'" +
            "    END AS outcome," +
            "    CASE WHEN status = 'COMPLETED' AND started_at IS NOT NULL AND completed_at IS NOT NULL" +
            "         THEN EXTRACT(EPOCH FROM completed_at - started_at) END AS duration_s" +
            "  FROM processes" +
            "  WHERE (status = 'ACTIVE' OR (completed_at IS NOT NULL AND completed_at >= ?))";

    private final JdbcOperations jdbcOperations;

    @Inject
    public AnalyticsService(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    /**
     * Computes per-tag deadline outcomes.
     *
     * @param name        optional process name filter
     * @param windowHours how far back to include finished processes
     * @return per-tag impact breakdown, most problematic tags first
     */
    @Transactional
    public TagImpactResponse getTagImpact(@Nullable String name, int windowHours) {
        Instant now = Instant.now();
        Instant since = now.minus(Duration.ofHours(windowHours));

        TagImpactResponse response = new TagImpactResponse();
        response.setWindowHours(windowHours);
        response.setGeneratedAt(now.getEpochSecond());
        loadTotals(response, name, now, since);
        response.setTags(loadTagRollup(name, now, since));
        return response;
    }

    private void loadTotals(TagImpactResponse response, @Nullable String name, Instant now, Instant since) {
        String sql = scopedCte(name) +
                "SELECT COUNT(*) AS total," +
                "  COUNT(*) FILTER (WHERE outcome IN ('OVERDUE','COMPLETED_LATE','FAILED')) AS problems," +
                "  COUNT(duration_s) AS dur_count," +
                "  AVG(duration_s) AS avg_s," +
                "  percentile_cont(0.5) WITHIN GROUP (ORDER BY duration_s) AS p50_s," +
                "  percentile_cont(0.9) WITHIN GROUP (ORDER BY duration_s) AS p90_s," +
                "  percentile_cont(0.99) WITHIN GROUP (ORDER BY duration_s) AS p99_s " +
                "FROM scoped";

        jdbcOperations.prepareStatement(sql, statement -> {
            bindScope(statement, name, now, since);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                response.setTotalProcesses(rs.getLong("total"));
                response.setProblemProcesses(rs.getLong("problems"));
                response.setDuration(readDurationStats(rs));
            }
            return null;
        });
    }

    private List<TagImpactEntry> loadTagRollup(@Nullable String name, Instant now, Instant since) {
        String sql = scopedCte(name) +
                "SELECT t.elem->>'key' AS tag_key, t.elem->>'value' AS tag_value," +
                "  COUNT(*) AS total," +
                "  COUNT(*) FILTER (WHERE outcome = 'OVERDUE') AS overdue," +
                "  COUNT(*) FILTER (WHERE outcome = 'ON_TRACK') AS on_track," +
                "  COUNT(*) FILTER (WHERE outcome = 'COMPLETED_LATE') AS completed_late," +
                "  COUNT(*) FILTER (WHERE outcome = 'COMPLETED_ON_TIME') AS completed_on_time," +
                "  COUNT(*) FILTER (WHERE outcome = 'FAILED') AS failed," +
                "  COUNT(*) FILTER (WHERE outcome IN ('OVERDUE','COMPLETED_LATE','FAILED')) AS problems," +
                "  COUNT(duration_s) AS dur_count," +
                "  AVG(duration_s) AS avg_s," +
                "  percentile_cont(0.5) WITHIN GROUP (ORDER BY duration_s) AS p50_s," +
                "  percentile_cont(0.9) WITHIN GROUP (ORDER BY duration_s) AS p90_s," +
                "  percentile_cont(0.99) WITHIN GROUP (ORDER BY duration_s) AS p99_s " +
                "FROM scoped s " +
                "CROSS JOIN LATERAL jsonb_array_elements(s.tags) AS t(elem) " +
                "WHERE jsonb_typeof(s.tags) = 'array' " +
                "GROUP BY t.elem->>'key', t.elem->>'value' " +
                "ORDER BY problems DESC, total DESC, tag_key ASC, tag_value ASC " +
                "LIMIT " + MAX_TAG_ROWS;

        return jdbcOperations.prepareStatement(sql, statement -> {
            bindScope(statement, name, now, since);
            List<TagImpactEntry> rows = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    TagImpactEntry entry = new TagImpactEntry(rs.getString("tag_key"), rs.getString("tag_value"));
                    entry.setTotal(rs.getLong("total"));
                    entry.setOverdue(rs.getLong("overdue"));
                    entry.setOnTrack(rs.getLong("on_track"));
                    entry.setCompletedLate(rs.getLong("completed_late"));
                    entry.setCompletedOnTime(rs.getLong("completed_on_time"));
                    entry.setFailed(rs.getLong("failed"));
                    entry.setProblems(rs.getLong("problems"));
                    entry.setDuration(readDurationStats(rs));
                    rows.add(entry);
                }
            }
            return rows;
        });
    }

    /**
     * Workspace-wide headline counts in a single aggregate pass: status
     * totals, deadline outcomes, and last-24h completion stats.
     */
    @Transactional
    public SummaryResponse getSummary() {
        Instant now = Instant.now();
        Instant dayAgo = now.minus(Duration.ofHours(24));
        String sql = "SELECT COUNT(*) AS total," +
                "  COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active," +
                "  COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed," +
                "  COUNT(*) FILTER (WHERE status = 'FAILED') AS failed," +
                "  COUNT(*) FILTER (WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline < ?) AS overdue," +
                "  COUNT(*) FILTER (WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline >= ?) AS on_track," +
                "  COUNT(*) FILTER (WHERE status = 'COMPLETED' AND deadline IS NOT NULL AND completed_at <= deadline) AS completed_on_time," +
                "  COUNT(*) FILTER (WHERE status = 'COMPLETED' AND deadline IS NOT NULL AND completed_at > deadline) AS completed_late," +
                "  COUNT(*) FILTER (WHERE status = 'COMPLETED' AND completed_at >= ?) AS completed_24h," +
                "  COUNT(*) FILTER (WHERE status = 'COMPLETED' AND completed_at >= ? AND deadline IS NOT NULL AND completed_at <= deadline) AS completed_on_time_24h," +
                "  COUNT(*) FILTER (WHERE status = 'FAILED' AND completed_at >= ?) AS failed_24h " +
                "FROM processes";

        return jdbcOperations.prepareStatement(sql, statement -> {
            statement.setTimestamp(1, Timestamp.from(now));
            statement.setTimestamp(2, Timestamp.from(now));
            statement.setTimestamp(3, Timestamp.from(dayAgo));
            statement.setTimestamp(4, Timestamp.from(dayAgo));
            statement.setTimestamp(5, Timestamp.from(dayAgo));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                SummaryResponse summary = new SummaryResponse();
                summary.setGeneratedAt(now.getEpochSecond());
                summary.setTotal(rs.getLong("total"));
                summary.setActive(rs.getLong("active"));
                summary.setCompleted(rs.getLong("completed"));
                summary.setFailed(rs.getLong("failed"));
                summary.setOverdue(rs.getLong("overdue"));
                summary.setOnTrack(rs.getLong("on_track"));
                summary.setCompletedOnTime(rs.getLong("completed_on_time"));
                summary.setCompletedLate(rs.getLong("completed_late"));
                summary.setCompleted24h(rs.getLong("completed_24h"));
                summary.setCompletedOnTime24h(rs.getLong("completed_on_time_24h"));
                summary.setFailed24h(rs.getLong("failed_24h"));
                return summary;
            }
        });
    }

    /**
     * Per-name run rollups (GROUP BY name), busiest names first, paginated.
     */
    @Transactional
    public PagedResult<NameRollupEntry> getNameRollups(int limit, int offset) {
        Instant now = Instant.now();
        String sql = "SELECT name, COUNT(*) AS total," +
                "  COUNT(*) FILTER (WHERE status = 'ACTIVE') AS active," +
                "  COUNT(*) FILTER (WHERE status = 'FAILED') AS failed," +
                "  COUNT(*) FILTER (WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline < ?) AS overdue," +
                "  COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed," +
                "  COUNT(*) FILTER (WHERE status = 'COMPLETED' AND deadline IS NOT NULL AND completed_at <= deadline) AS completed_on_time," +
                "  COUNT(*) FILTER (WHERE status = 'COMPLETED' AND deadline IS NOT NULL AND completed_at > deadline) AS completed_late," +
                "  MAX(started_at) AS last_started_at " +
                "FROM processes GROUP BY name " +
                "ORDER BY COUNT(*) DESC, name ASC LIMIT ? OFFSET ?";

        List<NameRollupEntry> rows = jdbcOperations.prepareStatement(sql, statement -> {
            statement.setTimestamp(1, Timestamp.from(now));
            statement.setInt(2, limit);
            statement.setInt(3, offset);
            List<NameRollupEntry> entries = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    NameRollupEntry entry = new NameRollupEntry();
                    entry.setName(rs.getString("name"));
                    entry.setTotal(rs.getLong("total"));
                    entry.setActive(rs.getLong("active"));
                    entry.setFailed(rs.getLong("failed"));
                    entry.setOverdue(rs.getLong("overdue"));
                    entry.setCompleted(rs.getLong("completed"));
                    entry.setCompletedOnTime(rs.getLong("completed_on_time"));
                    entry.setCompletedLate(rs.getLong("completed_late"));
                    Timestamp lastStarted = rs.getTimestamp("last_started_at");
                    entry.setLastStartedAt(lastStarted != null ? lastStarted.toInstant().getEpochSecond() : null);
                    entries.add(entry);
                }
            }
            return entries;
        });

        long totalNames = jdbcOperations.prepareStatement("SELECT COUNT(DISTINCT name) FROM processes", statement -> {
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        });

        return new PagedResult<>(rows, totalNames, limit, offset);
    }

    private String scopedCte(@Nullable String name) {
        StringBuilder cte = new StringBuilder(SCOPED_CTE_PREFIX);
        if (name != null) {
            cte.append(" AND name = ?");
        }
        return cte.append(") ").toString();
    }

    private void bindScope(PreparedStatement statement, @Nullable String name, Instant now, Instant since)
            throws SQLException {
        statement.setTimestamp(1, Timestamp.from(now));
        statement.setTimestamp(2, Timestamp.from(since));
        if (name != null) {
            statement.setString(3, name);
        }
    }

    /**
     * Reads dur_count/avg_s/p50_s/p90_s/p99_s columns into DurationStats;
     * null when the group has no finished runs, so the field is omitted.
     * Percentiles come from percentile_cont (interpolated); values are rounded
     * to one decimal like the former in-memory implementation's average.
     */
    private DurationStats readDurationStats(ResultSet rs) throws SQLException {
        long count = rs.getLong("dur_count");
        if (count == 0) {
            return null;
        }
        DurationStats stats = new DurationStats();
        stats.setCount(count);
        stats.setAvgSeconds(round1(rs.getDouble("avg_s")));
        stats.setP50Seconds(round1(rs.getDouble("p50_s")));
        stats.setP90Seconds(round1(rs.getDouble("p90_s")));
        stats.setP99Seconds(round1(rs.getDouble("p99_s")));
        return stats;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
