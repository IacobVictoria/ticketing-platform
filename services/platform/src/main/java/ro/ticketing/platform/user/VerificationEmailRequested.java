package ro.ticketing.platform.user;

import java.util.UUID;

/** Published inside the transaction that saved the token; {@link AccountEmailSender} acts on it after commit. */
record VerificationEmailRequested(UUID userId, String email, String rawToken) {
}
