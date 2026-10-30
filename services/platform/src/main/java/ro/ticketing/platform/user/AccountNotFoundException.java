package ro.ticketing.platform.user;

import ro.ticketing.platform.error.UnauthorizedException;

import java.util.Map;

/** The access token is valid, but its user no longer exists (deleted after the token was issued). */
class AccountNotFoundException extends UnauthorizedException {

    AccountNotFoundException() {
        super("ACCOUNT_NOT_FOUND", "The account for this token no longer exists", Map.of());
    }
}
