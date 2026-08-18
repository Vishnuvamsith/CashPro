CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY,
                               aggregate_type VARCHAR(100) NOT NULL,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(30) NOT NULL,
                               created_at TIMESTAMP NOT NULL,
                               published_at TIMESTAMP
);