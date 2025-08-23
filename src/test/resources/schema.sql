-- H2-compatible schema for testing
CREATE TABLE processes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    completed_at TIMESTAMP,
    deadline TIMESTAMP,
    tags CLOB,
    context CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP()
);

-- Add check constraint for valid status values
ALTER TABLE processes ADD CONSTRAINT chk_processes_status 
    CHECK (status IN ('ACTIVE', 'COMPLETED', 'FAILED'));