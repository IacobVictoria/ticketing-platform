CREATE TABLE venues (
    id         UUID         PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    city       VARCHAR(100) NOT NULL,
    address    VARCHAR(300),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE sectors (
    id         UUID         PRIMARY KEY,
    venue_id   UUID         NOT NULL REFERENCES venues (id),
    name       VARCHAR(100) NOT NULL,
    kind       VARCHAR(20)  NOT NULL,
    capacity   INTEGER,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,

    CONSTRAINT sectors_venue_name_unique UNIQUE (venue_id, name),
    CONSTRAINT sectors_kind_check CHECK (kind IN ('SEATED', 'STANDING')),
    -- standing areas have a capacity, seated ones are counted from their seats.
    -- "IS NOT NULL" is required: NULL > 0 is NULL, and a CHECK only rejects FALSE.
    CONSTRAINT sectors_capacity_check CHECK (
        (kind = 'STANDING' AND capacity IS NOT NULL AND capacity > 0)
        OR (kind = 'SEATED' AND capacity IS NULL)
    )
);

CREATE TABLE seats (
    id          UUID        PRIMARY KEY,
    sector_id   UUID        NOT NULL REFERENCES sectors (id),
    row_label   VARCHAR(10) NOT NULL,
    seat_number INTEGER     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT seats_position_unique UNIQUE (sector_id, row_label, seat_number),
    CONSTRAINT seats_number_check CHECK (seat_number > 0)
);
