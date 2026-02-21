CREATE TABLE dead_letter_queue (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    operation_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    error_message TEXT NOT NULL,
    stack_trace TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_retried_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_dlq_status CHECK (status IN ('PENDING', 'RETRIED', 'FAILED', 'RESOLVED'))
);

CREATE INDEX idx_dlq_status ON dead_letter_queue(status);
CREATE INDEX idx_dlq_created_at ON dead_letter_queue(created_at DESC);
