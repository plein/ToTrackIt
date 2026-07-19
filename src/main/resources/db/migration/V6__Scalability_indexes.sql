-- Indexes for the SQL-side list/filter/paginate path and the batched
-- deadline scanners. Composite orderings include the id DESC tiebreak so the
-- hot list pages are pure ordered index scans (no top-N sort).

-- Default listing order (status chip + newest first) and per-status pages.
CREATE INDEX idx_processes_status_started_at ON processes (status, started_at DESC, id DESC);

-- Per-name pages sorted by recency (process detail view).
CREATE INDEX idx_processes_name_started_at ON processes (name, started_at DESC, id DESC);

-- Unfiltered default page: replace the V3 started_at index with one that
-- also carries the tiebreak.
DROP INDEX IF EXISTS idx_processes_started_at;
CREATE INDEX idx_processes_started_at ON processes (started_at DESC, id DESC);

-- Warning scanner: active runs with a future deadline that have not been
-- warned yet. Mirrors idx_processes_deadline_unnotified (V4) for the missed
-- pass.
CREATE INDEX idx_processes_deadline_unwarned ON processes (deadline)
    WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline_warned_at IS NULL;

-- The context column is never queried; the GIN index only amplified writes.
DROP INDEX IF EXISTS idx_processes_context;
