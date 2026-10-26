package ro.ticketing.platform.event;

import ro.ticketing.platform.error.BusinessRuleException;

import java.time.Instant;
import java.util.Map;

class EventStartInPastException extends BusinessRuleException {

    EventStartInPastException(Instant startsAt) {
        super("EVENT_START_IN_PAST", "Event must start in the future", Map.of("startsAt", startsAt));
    }
}
