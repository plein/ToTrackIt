-- namespace_id: reserved for future multi-tenancy support. Nullable and unused by
-- the application today; kept in the schema so tenant scoping can be added without
-- a disruptive migration of populated tables.
ALTER TABLE processes ADD COLUMN namespace_id BIGINT;

-- deadline_notified_at: set when a deadline-breach webhook notification has been
-- sent for this process (NULL = never notified). Prevents duplicate notifications.
ALTER TABLE processes ADD COLUMN deadline_notified_at TIMESTAMP WITH TIME ZONE;

-- Partial index for the notification scanner: active processes past deadline that
-- have not yet been notified.
CREATE INDEX idx_processes_deadline_unnotified
    ON processes (deadline)
    WHERE status = 'ACTIVE' AND deadline IS NOT NULL AND deadline_notified_at IS NULL;
