package ro.ticketing.platform.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/** venueId is optional: a draft can be created first and get its venue later (PUT /api/events/{id}/venue). */
record CreateEventRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        @NotNull Instant startsAt,
        UUID venueId
) {
}
