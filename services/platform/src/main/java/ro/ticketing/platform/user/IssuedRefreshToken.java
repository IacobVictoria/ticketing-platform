package ro.ticketing.platform.user;

import java.time.Duration;

/** The raw token (goes into the cookie, never into the database) and how long it stays valid. */
record IssuedRefreshToken(String value, Duration ttl) {
}
