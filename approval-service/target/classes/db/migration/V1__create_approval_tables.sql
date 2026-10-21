CREATE TABLE approval_requests (
    payment_id UUID PRIMARY KEY,
    maker_id VARCHAR(100) NOT NULL,
    client_id VARCHAR(50) NOT NULL,
    amount NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version_tag BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE approval_decisions (
    decision_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE REFERENCES approval_requests(payment_id),
    maker_id VARCHAR(100) NOT NULL,
    approver_id VARCHAR(100) NOT NULL,
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED')),
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(500),
    CONSTRAINT chk_approval_maker_checker_separation CHECK (maker_id <> approver_id)
);

CREATE TABLE outbox_events (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (aggregate_id, event_type)
);

CREATE INDEX idx_approval_requests_status ON approval_requests(status);
CREATE INDEX idx_approval_decisions_decided_at ON approval_decisions(decided_at);
CREATE INDEX idx_approval_outbox_pending ON outbox_events(status, created_at);
