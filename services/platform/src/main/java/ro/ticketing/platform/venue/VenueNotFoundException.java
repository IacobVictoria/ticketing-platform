package ro.ticketing.platform.venue;

import ro.ticketing.platform.error.NotFoundException;

import java.util.Map;
import java.util.UUID;

class VenueNotFoundException extends NotFoundException {

    VenueNotFoundException(UUID venueId) {
        super("VENUE_NOT_FOUND", "Venue not found: " + venueId, Map.of("venueId", venueId));
    }
}
