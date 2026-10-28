package ro.ticketing.platform.venue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A venue with its sectors and the seat count of each sector, read from one database snapshot. */
record VenueDetails(Venue venue, List<Sector> sectors, Map<UUID, Long> seatCounts) {
}
