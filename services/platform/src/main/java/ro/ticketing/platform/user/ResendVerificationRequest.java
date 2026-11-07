package ro.ticketing.platform.user;

import jakarta.validation.constraints.NotBlank;

record ResendVerificationRequest(@NotBlank String email) {
}
