package ro.ticketing.platform.event;

import ro.ticketing.platform.error.ConflictException;

import java.util.Map;
import java.util.UUID;

class InvalidEventStateException extends ConflictException {

    InvalidEventStateException(UUID eventId, EventStatus currentStatus, String action) {
        super("EVENT_INVALID_STATE",
                "Cannot %s an event with status %s".formatted(action, currentStatus),
                Map.of("eventId", eventId, "currentStatus", currentStatus));
    }
}
