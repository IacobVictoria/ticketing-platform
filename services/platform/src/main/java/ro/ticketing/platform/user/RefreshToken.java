package ro.ticketing.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ro.ticketing.platform.common.BaseEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
class RefreshToken extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, updatable = false)
    private UUID familyId;

    /** SHA-256 of the token, hex. The token itself only exists in the user's cookie. */
    @Column(nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false, updatable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RevokeReason revokeReason;

    protected RefreshToken() {
    }

    RefreshToken(UUID userId, UUID familyId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    boolean isRevoked() {
        return revokedAt != null;
    }

    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    /** Rotated at or after {@code instant}: typically another tab refreshed with the same cookie a moment ago. */
    boolean wasRotatedSince(Instant instant) {
        return revokeReason == RevokeReason.ROTATED && !revokedAt.isBefore(instant);
    }

    /** Keeps the first reason: revoking an already revoked token changes nothing. */
    void revoke(RevokeReason reason, Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
            revokeReason = reason;
        }
    }

    UUID getUserId() {
        return userId;
    }

    UUID getFamilyId() {
        return familyId;
    }

    RevokeReason getRevokeReason() {
        return revokeReason;
    }
}
