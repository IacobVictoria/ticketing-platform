package ro.ticketing.platform.user;

import ro.ticketing.platform.error.UnauthorizedException;

import java.util.Map;

/** Same message for unknown email and wrong password, so the response does not reveal which accounts exist. */
class InvalidCredentialsException extends UnauthorizedException {

    InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid email or password", Map.of());
    }
}
