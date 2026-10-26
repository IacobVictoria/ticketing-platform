package ro.ticketing.platform.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

record CreateEventRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 5000) String description,
        @NotNull Instant startsAt
) {
}
