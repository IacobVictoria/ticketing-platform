package ro.ticketing.platform.user;

import ro.ticketing.platform.error.ForbiddenException;

import java.util.Map;

/** Thrown only after the password was correct, so it tells nothing to someone guessing passwords. */
class EmailNotVerifiedException extends ForbiddenException {

    EmailNotVerifiedException() {
        super("EMAIL_NOT_VERIFIED", "Please confirm your email address first", Map.of());
    }
}
