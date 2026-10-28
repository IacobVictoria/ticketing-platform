package ro.ticketing.platform.venue;

import java.util.UUID;

/** One row of the "seats per sector" query. */
record SectorSeatCount(UUID sectorId, long seats) {
}
