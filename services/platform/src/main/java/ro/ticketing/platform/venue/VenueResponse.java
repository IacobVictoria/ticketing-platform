package ro.ticketing.platform.venue;

import java.time.Instant;
import java.util.UUID;

record VenueResponse(UUID id, String name, String city, String address, Instant createdAt, Instant updatedAt) {

    static VenueResponse from(Venue venue) {
        return new VenueResponse(venue.getId(), venue.getName(), venue.getCity(), venue.getAddress(),
                venue.getCreatedAt(), venue.getUpdatedAt());
    }
}
