package ro.ticketing.platform.event;

import java.time.Instant;
import java.util.UUID;

record EventResponse(
        UUID id,
        String name,
        String description,
        Instant startsAt,
        EventStatus status,
        UUID venueId,
        Instant createdAt,
        Instant updatedAt
) {

    static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartsAt(),
                event.getStatus(),
                event.getVenueId(),
                event.getCreatedAt(),
                event.getUpdatedAt());
    }
}
