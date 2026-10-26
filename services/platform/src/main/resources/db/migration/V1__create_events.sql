CREATE TABLE events (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    starts_at   TIMESTAMPTZ  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT events_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED'))
);

CREATE INDEX events_starts_at_idx ON events (starts_at);
