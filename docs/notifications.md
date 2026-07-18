# Notifications

Set `TOTRACKIT_WEBHOOK_URL` to enable deadline notifications. A background scanner (every 60s by default, tunable via `totrackit.notification-scan-interval`) fires two kinds of events, each at most once per process.

## Deadline missed

When an active process passes its deadline, the webhook receives a JSON POST:

```json
{
  "event": "process.deadline_missed",
  "name": "dataImport",
  "id": "batch42",
  "started_at": 1699990000,
  "deadline": 1699999999,
  "tags": [{ "key": "env", "value": "prod" }],
  "context": { "customerId": "C1234" },
  "url": "https://totrackit.internal.example.com/?process=dataImport/batch42"
}
```

Any 2xx response marks the process as notified; failed deliveries are retried on the next scan. Point it at Slack (via a bridge), your incident tooling, or any HTTP endpoint.

## Pre-deadline warning

Before a breach ever happens, a `process.deadline_warning` event fires when an active run crosses the warning threshold: a fraction of its deadline budget, 75% by default. The payload has the same shape plus `seconds_remaining`, so automation can act while there is still time. Retry the job, escalate it, or warn the customer proactively.

```json
{
  "event": "process.deadline_warning",
  "name": "dataImport",
  "id": "batch42",
  "started_at": 1699990000,
  "deadline": 1699999999,
  "seconds_remaining": 900,
  "tags": [{ "key": "env", "value": "prod" }],
  "context": { "customerId": "C1234" },
  "url": "https://totrackit.internal.example.com/?process=dataImport/batch42"
}
```

Tune the threshold with `totrackit.warning-threshold` (or the `TOTRACKIT_WARNING_THRESHOLD` environment variable). Values must be between 0 and 1 exclusive; anything else disables warnings. Both event types share the receiver, so filter on the `event` field.

The `url` field is included when `TOTRACKIT_PUBLIC_URL` is set to the public base URL of the dashboard. It deep-links straight to the impacted process, so whoever receives the alert is one click away from the process page and the impacted-tags view.

The deadline scanner runs regardless of webhook configuration; it also feeds the `totrackit_processes_deadline_missed_total` and `totrackit_processes_deadline_warning_total` metrics (see [Metrics](metrics.md)).

Email and other notification channels are on the [roadmap](../README.md#%EF%B8%8F-roadmap).
