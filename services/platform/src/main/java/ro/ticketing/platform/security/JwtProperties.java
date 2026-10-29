package ro.ticketing.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Bound from {@code app.jwt.*} in application.yml. */
@ConfigurationProperties("app.jwt")
record JwtProperties(String issuer, String secret, Duration ttl) {

    /** HS256 signs with a 256-bit key; a shorter secret is easier to brute-force and Nimbus refuses it. */
    static final int MIN_SECRET_BYTES = 32;

    /** Fail at startup, not at the first login. */
    JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("app.jwt.issuer is required");
        }
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("app.jwt.secret must be at least %d bytes".formatted(MIN_SECRET_BYTES));
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("app.jwt.ttl must be positive");
        }
    }
}
