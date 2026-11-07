package ro.ticketing.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import ro.ticketing.platform.common.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationToken extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID userId;

    /** SHA-256 of the token; the token itself is only in the link from the email. */
    @Column(nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    protected EmailVerificationToken() {
    }

    EmailVerificationToken(UUID userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    UUID getUserId() {
        return userId;
    }
}
