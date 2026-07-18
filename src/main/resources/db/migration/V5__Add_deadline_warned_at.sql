-- Pre-deadline warning support: tracks when the approaching-deadline warning
-- was processed for a run, so it fires at most once per process.
ALTER TABLE processes ADD COLUMN deadline_warned_at TIMESTAMP WITH TIME ZONE;
