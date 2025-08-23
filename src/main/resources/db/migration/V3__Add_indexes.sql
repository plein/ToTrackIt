-- Add performance indexes for common query patterns

-- Index for finding processes by name (used in most queries)
CREATE INDEX idx_processes_name ON processes (name);

-- Index for filtering by status
CREATE INDEX idx_processes_status ON processes (status);

-- Index for deadline-based queries and sorting
CREATE INDEX idx_processes_deadline ON processes (deadline);

-- Index for sorting by creation time (default sort order)
CREATE INDEX idx_processes_started_at ON processes (started_at DESC);

-- Index for completion time queries and metrics
CREATE INDEX idx_processes_completed_at ON processes (completed_at) WHERE completed_at IS NOT NULL;

-- Composite index for common filtering combinations
CREATE INDEX idx_processes_name_status ON processes (name, status);

-- Index for deadline status calculations (processes with deadlines)
CREATE INDEX idx_processes_deadline_status ON processes (deadline, status) WHERE deadline IS NOT NULL;

-- GIN index for JSONB tags column to support tag-based filtering
CREATE INDEX idx_processes_tags ON processes USING GIN (tags);

-- GIN index for JSONB context column for flexible context queries
CREATE INDEX idx_processes_context ON processes USING GIN (context);