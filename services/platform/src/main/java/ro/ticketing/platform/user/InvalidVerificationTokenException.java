package ro.ticketing.platform.user;

import ro.ticketing.platform.error.BusinessRuleException;

import java.util.Map;

/** Unknown, already used or expired link: the user can ask for a new email. */
class InvalidVerificationTokenException extends BusinessRuleException {

    InvalidVerificationTokenException() {
        super("INVALID_VERIFICATION_TOKEN", "This verification link is invalid or has expired", Map.of());
    }
}
