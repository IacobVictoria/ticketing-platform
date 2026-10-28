package ro.ticketing.platform.event;

import ro.ticketing.platform.error.BusinessRuleException;

import java.util.Map;
import java.util.UUID;

class EventVenueMissingException extends BusinessRuleException {

    EventVenueMissingException(UUID eventId) {
        super("EVENT_VENUE_MISSING", "An event needs a venue before it can be published", Map.of("eventId", eventId));
    }
}
