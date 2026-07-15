-- H2-compatible schema for testing.
-- Runs on EVERY new connection (INIT=RUNSCRIPT in the datasource URL), so every
-- statement must be idempotent.
CREATE TABLE IF NOT EXISTS processes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    completed_at TIMESTAMP,
    deadline TIMESTAMP,
    tags CLOB,
    context CLOB,
    namespace_id BIGINT,
    deadline_notified_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

-- Add check constraint for valid status values
ALTER TABLE processes ADD CONSTRAINT IF NOT EXISTS chk_processes_status
    CHECK (status IN ('ACTIVE', 'COMPLETED', 'FAILED'));