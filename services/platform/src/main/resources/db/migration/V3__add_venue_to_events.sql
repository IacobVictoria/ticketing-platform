-- nullable: a DRAFT event may not have a venue yet; publishing requires one
ALTER TABLE events ADD COLUMN venue_id UUID REFERENCES venues (id);

CREATE INDEX events_venue_id_idx ON events (venue_id);
