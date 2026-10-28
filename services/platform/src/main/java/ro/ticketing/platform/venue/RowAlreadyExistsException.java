package ro.ticketing.platform.venue;

import ro.ticketing.platform.error.ConflictException;

import java.util.Map;
import java.util.UUID;

class RowAlreadyExistsException extends ConflictException {

    RowAlreadyExistsException(UUID sectorId, String rowLabel) {
        super("ROW_ALREADY_EXISTS", "Sector already has a row '%s'".formatted(rowLabel),
                Map.of("sectorId", sectorId, "rowLabel", rowLabel));
    }
}
