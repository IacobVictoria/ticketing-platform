package ro.ticketing.platform.venue;

import ro.ticketing.platform.error.NotFoundException;

import java.util.Map;
import java.util.UUID;

class SectorNotFoundException extends NotFoundException {

    SectorNotFoundException(UUID venueId, UUID sectorId) {
        super("SECTOR_NOT_FOUND", "Sector %s not found in venue %s".formatted(sectorId, venueId),
                Map.of("venueId", venueId, "sectorId", sectorId));
    }
}
