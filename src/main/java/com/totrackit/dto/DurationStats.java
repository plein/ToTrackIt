package com.totrackit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/**
 * Completion-duration statistics (seconds) over a set of finished runs.
 * Percentiles use the nearest-rank method.
 */
@Introspected
@Serdeable
public class DurationStats {

    @JsonProperty("count")
    private long count;

    @JsonProperty("avg_seconds")
    private double avgSeconds;

    @JsonProperty("p50_seconds")
    private double p50Seconds;

    @JsonProperty("p90_seconds")
    private double p90Seconds;

    @JsonProperty("p99_seconds")
    private double p99Seconds;

    public DurationStats() {}

    /**
     * Builds stats from raw durations in seconds; returns null when empty so
     * the field is omitted for tags with no completed runs.
     */
    public static DurationStats of(List<Long> durationsSeconds) {
        if (durationsSeconds == null || durationsSeconds.isEmpty()) {
            return null;
        }
        List<Long> sorted = durationsSeconds.stream().sorted().toList();
        DurationStats stats = new DurationStats();
        stats.count = sorted.size();
        stats.avgSeconds = Math.round(sorted.stream().mapToLong(Long::longValue).average().orElse(0) * 10.0) / 10.0;
        stats.p50Seconds = percentile(sorted, 50);
        stats.p90Seconds = percentile(sorted, 90);
        stats.p99Seconds = percentile(sorted, 99);
        return stats;
    }

    private static double percentile(List<Long> sorted, int pct) {
        int rank = (int) Math.ceil(pct / 100.0 * sorted.size());
        return sorted.get(Math.max(0, rank - 1));
    }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public double getAvgSeconds() { return avgSeconds; }
    public void setAvgSeconds(double avgSeconds) { this.avgSeconds = avgSeconds; }

    public double getP50Seconds() { return p50Seconds; }
    public void setP50Seconds(double p50Seconds) { this.p50Seconds = p50Seconds; }

    public double getP90Seconds() { return p90Seconds; }
    public void setP90Seconds(double p90Seconds) { this.p90Seconds = p90Seconds; }

    public double getP99Seconds() { return p99Seconds; }
    public void setP99Seconds(double p99Seconds) { this.p99Seconds = p99Seconds; }
}
