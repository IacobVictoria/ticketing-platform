package ro.ticketing.platform.venue;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record AddRowRequest(
        @NotBlank @Size(max = 10) String rowLabel,
        @Min(1) @Max(500) int seats
) {
}
