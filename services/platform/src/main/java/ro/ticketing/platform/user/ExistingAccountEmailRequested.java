package ro.ticketing.platform.user;

import java.util.UUID;

/** Someone registered with an email that already has a verified account; {@link AccountEmailSender} tells its owner. */
record ExistingAccountEmailRequested(UUID userId, String email) {
}
