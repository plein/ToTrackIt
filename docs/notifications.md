# Notifications

Set `TOTRACKIT_WEBHOOK_URL` to enable deadline-breach notifications. A background scanner (every 60s by default, tunable via `totrackit.notification-scan-interval`) finds active processes past their deadline and sends each one a single JSON POST:

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

The `url` field is included when `TOTRACKIT_PUBLIC_URL` is set to the public base URL of the dashboard — it deep-links straight to the impacted process, so whoever receives the alert is one click away from the process page and the impacted-tags view.

The deadline scanner runs regardless of webhook configuration; it also feeds the `totrackit_processes_deadline_missed_total` metric (see [Metrics](metrics.md)).

Email and other notification channels are on the [roadmap](../README.md#%EF%B8%8F-roadmap).
