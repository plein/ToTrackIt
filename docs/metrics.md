# Metrics, Prometheus & Datadog

ToTrackIt exposes Prometheus/OpenMetrics at `/prometheus`. Beyond HTTP and DB metrics, the deadline-aware metrics are designed to plug ToTrackIt into whatever alerting your team already pages on.

| Metric | Type | Labels | Meaning |
|---|---|---|---|
| `totrackit_processes_overdue_current` | gauge | `process_name` | Active processes currently past their deadline (updated every 30s) |
| `totrackit_processes_deadline_missed_total` | counter | `process_name` | Deadline breaches, counted once per process |
| `totrackit_processes_deadline_warning_total` | counter | `process_name` | Runs that crossed the pre-deadline warning threshold (default 75% of budget) |
| `totrackit_processes_completed_on_time_total` | counter | `process_name` | Completions within the deadline |
| `totrackit_processes_completed_late_total` | counter | `process_name` | Completions after the deadline |
| `totrackit_active_processes_current` | gauge | — | All currently active processes |

## Prometheus alert example

```yaml
- alert: ProcessOverdue
  expr: totrackit_processes_overdue_current > 0
  for: 1m
  annotations:
    summary: "{{ $labels.process_name }}: {{ $value }} process(es) past deadline"
```

## Datadog

The Datadog Agent scrapes `/prometheus` natively via its OpenMetrics integration. Add to `conf.d/openmetrics.d/conf.yaml`:

```yaml
instances:
  - openmetrics_endpoint: http://<totrackit-host>:8080/prometheus
    namespace: totrackit
    metrics:
      - totrackit_.*
```

Typical monitors:

* **Stuck processes.** A metric monitor on `totrackit.processes_overdue_current` `> 0` by `process_name`. Fires while anything is past its deadline; resolves when the backlog clears.
* **SLO / on-time rate.** Create a Datadog metric-based SLO with good events = `completed_on_time_total` and total events = `completed_on_time_total + completed_late_total + deadline_missed_total`. This gives you error budgets and burn-rate alerts on e.g. "99% of account activations complete within 1 hour".

When a monitor fires, the dashboard's **Impacted tags** panel (and `GET /analytics/tags`) shows which segment (country, locale, provider) the overdue and late processes are concentrated in.
