package ro.ticketing.platform.security;

/** A signed JWT and how many seconds it stays valid. */
public record IssuedToken(String value, long expiresInSeconds) {
}
