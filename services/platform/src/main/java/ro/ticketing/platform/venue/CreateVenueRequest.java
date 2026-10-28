package ro.ticketing.platform.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record CreateVenueRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 300) String address
) {
}
