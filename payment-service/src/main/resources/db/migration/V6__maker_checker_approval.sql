ALTER TABLE payments
    ADD COLUMN maker_id VARCHAR(100);

CREATE TABLE payment_approval_decisions (
    decision_id UUID PRIMARY KEY,
    payment_id UUID NOT NULL UNIQUE REFERENCES payments(payment_id),
    maker_id VARCHAR(100) NOT NULL,
    approver_id VARCHAR(100) NOT NULL,
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED')),
    decided_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(500),
    CONSTRAINT chk_approval_maker_checker_separation CHECK (maker_id <> approver_id)
);

CREATE INDEX idx_payment_approval_decisions_decided_at
    ON payment_approval_decisions(decided_at);
