package ro.ticketing.platform.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTests {

    static final String VALID_SECRET = "a".repeat(JwtProperties.MIN_SECRET_BYTES);

    @Test
    void validPropertiesAreAccepted() {
        assertThatCode(() -> new JwtProperties("issuer", VALID_SECRET, Duration.ofHours(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void shortSecretIsRejected() {
        assertThatThrownBy(() -> new JwtProperties("issuer", "too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.jwt.secret");
    }

    @Test
    void missingIssuerOrNonPositiveTtlIsRejected() {
        assertThatThrownBy(() -> new JwtProperties(" ", VALID_SECRET, Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties("issuer", VALID_SECRET, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
