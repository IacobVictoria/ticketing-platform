package ro.ticketing.platform.event;

import ro.ticketing.platform.error.NotFoundException;

import java.util.Map;
import java.util.UUID;

class EventNotFoundException extends NotFoundException {

    EventNotFoundException(UUID id) {
        super("EVENT_NOT_FOUND", "Event not found: " + id, Map.of("eventId", id));
    }
}
