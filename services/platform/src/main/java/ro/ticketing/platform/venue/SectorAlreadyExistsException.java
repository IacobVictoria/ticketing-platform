package ro.ticketing.platform.venue;

import ro.ticketing.platform.error.ConflictException;

import java.util.Map;
import java.util.UUID;

class SectorAlreadyExistsException extends ConflictException {

    SectorAlreadyExistsException(UUID venueId, String name) {
        super("SECTOR_ALREADY_EXISTS", "Venue already has a sector named '%s'".formatted(name),
                Map.of("venueId", venueId, "name", name));
    }
}
