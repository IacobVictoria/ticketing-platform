package ro.ticketing.platform.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

record CreateSectorRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull SectorKind kind,
        @Positive Integer capacity
) {
}
