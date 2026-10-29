package ro.ticketing.platform.user;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bound from {@code app.refresh-token.*} in application.yml.
 *
 * @param ttl          how long a refresh token (and so a login) lasts without being used
 * @param reuseGrace   a token rotated less than this long ago is refused without revoking its family
 *                     (two tabs refreshing at the same moment is not theft)
 * @param cookieSecure send the cookie only over HTTPS; browsers treat http://localhost as secure too
 */
@ConfigurationProperties("app.refresh-token")
record RefreshTokenProperties(Duration ttl, Duration reuseGrace, boolean cookieSecure) {

    RefreshTokenProperties {
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("app.refresh-token.ttl must be positive");
        }
        if (reuseGrace == null || reuseGrace.isNegative()) {
            throw new IllegalArgumentException("app.refresh-token.reuse-grace must be zero or positive");
        }
    }
}
