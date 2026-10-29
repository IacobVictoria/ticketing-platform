package ro.ticketing.platform.user;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * The refresh token travels only in this cookie:
 * <ul>
 *   <li>HttpOnly – JavaScript cannot read it, so an XSS script cannot steal it;</li>
 *   <li>Secure – only sent over HTTPS;</li>
 *   <li>SameSite=Strict – not sent on requests started by another site, which is what blocks CSRF here;</li>
 *   <li>Path=/api/auth – sent only to the auth endpoints, not with every API call.</li>
 * </ul>
 */
@Component
class RefreshTokenCookies {

    static final String NAME = "refresh_token";
    private static final String PATH = "/api/auth";

    private final RefreshTokenProperties properties;

    RefreshTokenCookies(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    ResponseCookie create(IssuedRefreshToken token) {
        return cookie(token.value(), token.ttl());
    }

    /** Same name and path with Max-Age=0: the browser deletes it. */
    ResponseCookie clear() {
        return cookie("", Duration.ZERO);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}
