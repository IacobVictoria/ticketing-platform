-- The application stores sector names trimmed and row labels trimmed + upper-case,
-- so "A", "a" and "A " are the same row. These constraints reject anything that bypasses that.
UPDATE sectors SET name = btrim(name);
UPDATE seats SET row_label = upper(btrim(row_label));

-- "Tribuna 1" and "tribuna 1" are the same sector
ALTER TABLE sectors DROP CONSTRAINT sectors_venue_name_unique;
CREATE UNIQUE INDEX sectors_venue_name_unique ON sectors (venue_id, lower(name));
ALTER TABLE sectors ADD CONSTRAINT sectors_name_trimmed_check CHECK (name = btrim(name) AND name <> '');

ALTER TABLE seats ADD CONSTRAINT seats_row_label_normalized_check
    CHECK (row_label = upper(btrim(row_label)) AND row_label <> '');
