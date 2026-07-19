package com.totrackit.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.totrackit.dto.Pageable;
import com.totrackit.dto.ProcessFilter;
import com.totrackit.entity.ProcessEntity;
import com.totrackit.model.DeadlineStatus;
import io.micronaut.data.jdbc.runtime.JdbcOperations;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dynamic list/count queries for processes. Emits a WHERE predicate only for
 * filters that are actually present, so PostgreSQL can plan against the
 * matching index instead of the non-sargable {@code (:x IS NULL OR col = :x)}
 * idiom, and pushes tag containment, deadline-status translation, sorting and
 * pagination into SQL rather than loading the table into memory.
 */
@Singleton
public class ProcessQueryRepository {

    /** Whitelisted sort keys mapped to ORDER BY expressions. Never interpolate user input. */
    private static final Map<String, String> SORT_EXPRESSIONS = Map.of(
            "started_at", "started_at",
            "completed_at", "completed_at",
            "deadline", "deadline",
            "name", "name",
            "status", "status",
            "duration", "(COALESCE(completed_at, now()) - started_at)"
    );

    /**
     * Sort keys over nullable columns need an explicit NULLS LAST. NOT NULL
     * columns must NOT get one: "DESC NULLS LAST" doesn't match a plain DESC
     * index ordering, which would force a top-N sort instead of an ordered
     * index scan on the default started_at page.
     */
    private static final Set<String> NULLABLE_SORT_KEYS = Set.of("completed_at", "deadline");

    private final JdbcOperations jdbcOperations;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProcessQueryRepository(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    @Transactional
    public List<ProcessEntity> findPage(ProcessFilter filter, Pageable pageable) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        buildWhere(filter, clauses, params);

        StringBuilder sql = new StringBuilder("SELECT * FROM processes");
        appendWhere(sql, clauses);
        sql.append(" ORDER BY ").append(orderBy(filter));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageable.getLimit());
        params.add(pageable.getOffset());

        return jdbcOperations.prepareStatement(sql.toString(), statement -> {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return jdbcOperations.entityStream(rs, ProcessEntity.class).collect(Collectors.toList());
            }
        });
    }

    @Transactional
    public long count(ProcessFilter filter) {
        List<String> clauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        buildWhere(filter, clauses, params);

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM processes");
        appendWhere(sql, clauses);

        return jdbcOperations.prepareStatement(sql.toString(), statement -> {
            bind(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        });
    }

    /**
     * Counts currently-overdue active processes grouped by name, largest
     * groups first, capped so per-name gauge cardinality stays bounded.
     */
    @Transactional
    public Map<String, Long> countOverdueByName(Instant now, int limit) {
        String sql = "SELECT name, COUNT(*) AS overdue FROM processes " +
                "WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline < ? " +
                "GROUP BY name ORDER BY COUNT(*) DESC, name ASC LIMIT ?";
        return jdbcOperations.prepareStatement(sql, statement -> {
            statement.setTimestamp(1, Timestamp.from(now));
            statement.setInt(2, limit);
            Map<String, Long> counts = new LinkedHashMap<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("name"), rs.getLong("overdue"));
                }
            }
            return counts;
        });
    }

    private void buildWhere(ProcessFilter filter, List<String> clauses, List<Object> params) {
        if (filter.getName() != null) {
            clauses.add("name = ?");
            params.add(filter.getName());
        }
        if (filter.getId() != null) {
            clauses.add("process_id = ?");
            params.add(filter.getId());
        }
        if (filter.getStatus() != null) {
            clauses.add("status = ?");
            params.add(filter.getStatus().name());
        }
        if (filter.getTags() != null) {
            // tags is a JSONB array of {"key","value"} objects; one containment
            // predicate per requested pair, AND-composed, served by the GIN index.
            for (Map.Entry<String, String> tag : filter.getTags().entrySet()) {
                clauses.add("tags @> ?::jsonb");
                params.add(tagContainmentJson(tag.getKey(), tag.getValue()));
            }
        }
        if (filter.getDeadlineBefore() != null) {
            clauses.add("deadline < to_timestamp(?)");
            params.add(filter.getDeadlineBefore());
        }
        if (filter.getDeadlineAfter() != null) {
            clauses.add("deadline > to_timestamp(?)");
            params.add(filter.getDeadlineAfter());
        }
        if (filter.getDeadlineStatus() != null) {
            clauses.add(deadlineStatusPredicate(filter.getDeadlineStatus()));
        }
        if (filter.getRunningDurationMin() != null) {
            clauses.add("status = 'ACTIVE' AND started_at <= now() - make_interval(mins => ?)");
            params.add(filter.getRunningDurationMin());
        }
    }

    /**
     * Translates the read-time DeadlineStatus model into a SQL predicate over
     * the stored timestamps. The response field itself stays computed in Java.
     */
    private String deadlineStatusPredicate(DeadlineStatus status) {
        return switch (status) {
            case MISSED -> "status = 'ACTIVE' AND deadline IS NOT NULL AND deadline < now()";
            case ON_TRACK -> "status = 'ACTIVE' AND deadline IS NOT NULL AND deadline >= now()";
            case COMPLETED_ON_TIME -> "status = 'COMPLETED' AND deadline IS NOT NULL AND completed_at <= deadline";
            case COMPLETED_LATE -> "status = 'COMPLETED' AND deadline IS NOT NULL AND completed_at > deadline";
        };
    }

    private String orderBy(ProcessFilter filter) {
        String sortKey = filter.getSortBy() != null ? filter.getSortBy().toLowerCase() : "started_at";
        if (!SORT_EXPRESSIONS.containsKey(sortKey)) {
            sortKey = "started_at";
        }
        String expression = SORT_EXPRESSIONS.get(sortKey);
        String direction = "asc".equalsIgnoreCase(filter.getSortDirection()) ? "ASC" : "DESC";
        String nulls = NULLABLE_SORT_KEYS.contains(sortKey) ? " NULLS LAST" : "";
        return expression + " " + direction + nulls + ", id DESC";
    }

    private String tagContainmentJson(String key, String value) {
        try {
            return objectMapper.writeValueAsString(List.of(new LinkedHashMap<>(Map.of("key", key, "value", value))));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid tag filter: " + key + ":" + value, e);
        }
    }

    private void appendWhere(StringBuilder sql, List<String> clauses) {
        if (!clauses.isEmpty()) {
            sql.append(" WHERE (").append(String.join(") AND (", clauses)).append(")");
        }
    }

    private void bind(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof Instant instant) {
                statement.setTimestamp(i + 1, Timestamp.from(instant));
            } else {
                statement.setObject(i + 1, param);
            }
        }
    }
}
