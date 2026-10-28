package ro.ticketing.platform.venue;

import ro.ticketing.platform.error.BusinessRuleException;

import java.util.Map;
import java.util.UUID;

class SectorNotSeatedException extends BusinessRuleException {

    SectorNotSeatedException(UUID sectorId) {
        super("SECTOR_NOT_SEATED", "Rows of seats can only be added to a SEATED sector",
                Map.of("sectorId", sectorId));
    }
}
