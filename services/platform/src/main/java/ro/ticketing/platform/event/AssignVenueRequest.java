package ro.ticketing.platform.event;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record AssignVenueRequest(@NotNull UUID venueId) {
}
