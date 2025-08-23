-- Create processes table with complete schema
CREATE TABLE processes (
    id BIGSERIAL PRIMARY KEY,
    process_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    deadline TIMESTAMP WITH TIME ZONE,
    tags JSONB,
    context JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Add check constraint for valid status values
ALTER TABLE processes ADD CONSTRAINT chk_processes_status 
    CHECK (status IN ('ACTIVE', 'COMPLETED', 'FAILED'));

-- Add unique constraint for active processes (only one active process per name/process_id combination)
CREATE UNIQUE INDEX idx_processes_unique_active 
    ON processes (name, process_id) 
    WHERE status = 'ACTIVE';

-- Add trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_processes_updated_at 
    BEFORE UPDATE ON processes 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();