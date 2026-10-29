package ro.ticketing.platform.user;

import ro.ticketing.platform.error.UnauthorizedException;

import java.util.Map;

/** Missing, unknown, expired or revoked: the client cannot tell which, and should send the user to login. */
class InvalidRefreshTokenException extends UnauthorizedException {

    InvalidRefreshTokenException() {
        super("INVALID_REFRESH_TOKEN", "Session expired, please log in again", Map.of());
    }
}
