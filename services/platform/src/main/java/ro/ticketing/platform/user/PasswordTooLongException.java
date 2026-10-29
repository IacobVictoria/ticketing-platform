package ro.ticketing.platform.user;

import ro.ticketing.platform.error.BusinessRuleException;

import java.util.Map;

/**
 * BCrypt only uses the first 72 bytes of a password. @Size counts characters, and characters like "ă" take 2 bytes,
 * so a 72-character password can still be too long.
 */
class PasswordTooLongException extends BusinessRuleException {

    PasswordTooLongException(int maxBytes) {
        super("PASSWORD_TOO_LONG", "Password must be at most %d bytes long".formatted(maxBytes),
                Map.of("maxBytes", maxBytes));
    }
}
