-- Transactional outbox: events are written here in the same transaction as the business change,
-- and a relay publishes them to the message broker afterwards.
CREATE TABLE outbox_events (
    id             UUID         PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,   -- e.g. 'Book'
    aggregate_id   VARCHAR(100) NOT NULL,   -- e.g. the book id
    event_type     VARCHAR(100) NOT NULL,   -- e.g. 'BookCreated'
    payload        JSONB        NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ               -- NULL = not yet published
);

-- The relay only ever looks for unpublished events, oldest first.
CREATE INDEX ix_outbox_events_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
