package ro.ticketing.platform.venue;

import ro.ticketing.platform.error.BusinessRuleException;

import java.util.Map;

class InvalidSectorCapacityException extends BusinessRuleException {

    InvalidSectorCapacityException(String message) {
        super("SECTOR_CAPACITY_INVALID", message, Map.of());
    }
}
