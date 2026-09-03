ALTER TABLE outbox_events
ALTER COLUMN payload TYPE JSONB
USING payload::jsonb;