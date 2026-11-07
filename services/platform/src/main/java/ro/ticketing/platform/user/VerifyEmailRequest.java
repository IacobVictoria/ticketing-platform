package ro.ticketing.platform.user;

import jakarta.validation.constraints.NotBlank;

/** The token from the link in the email (?token=...) and the password chosen at registration. */
record VerifyEmailRequest(@NotBlank String token, @NotBlank String password) {
}
