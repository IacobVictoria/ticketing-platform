package ro.ticketing.platform.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh tokens are opaque random strings (not JWTs): they are only ever checked here, against the database,
 * which is what makes them revocable.
 * <p>
 * Every refresh rotates the token: the old one is revoked and a new one issued in the same family.
 * If a revoked token comes back, two parties hold copies (the user and a thief), and we cannot tell which
 * is which, so the whole family is revoked and both must log in again.
 */
@Service
@Transactional
class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** 256 bits of randomness: guessing a valid token is not feasible. */
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties properties;
    private final Clock clock;

    RefreshTokenService(RefreshTokenRepository refreshTokenRepository, RefreshTokenProperties properties,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
        this.clock = clock;
    }

    /** At login: a new session, so a new family. */
    IssuedRefreshToken issueForLogin(UUID userId) {
        return issue(userId, UUID.randomUUID());
    }

    /**
     * noRollbackFor: when reuse is detected we revoke the family and then throw;
     * without it the exception would roll back the revocation too.
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    Rotation rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokenRepository.findForUpdateByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (token.isRevoked()) {
            // within the grace period it is most likely another tab of the same user; it simply retries
            boolean reusedAfterRotation = token.getRevokeReason() == RevokeReason.ROTATED
                    && !token.wasRotatedSince(now.minus(properties.reuseGrace()));
            if (reusedAfterRotation) {
                log.warn("Refresh token reuse detected [userId={}, familyId={}]; revoking the family",
                        token.getUserId(), token.getFamilyId());
                refreshTokenRepository.revokeFamily(token.getFamilyId(), RevokeReason.REUSE_DETECTED, now);
            }
            throw new InvalidRefreshTokenException();
        }
        if (token.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        token.revoke(RevokeReason.ROTATED, now);
        return new Rotation(token.getUserId(), issue(token.getUserId(), token.getFamilyId()));
    }

    /** Logout. Unknown or already revoked tokens are ignored: logging out twice is not an error. */
    void revoke(String rawToken) {
        refreshTokenRepository.findForUpdateByTokenHash(hash(rawToken))
                .ifPresent(token -> token.revoke(RevokeReason.LOGOUT, clock.instant()));
    }

    /**
     * Only expired rows are deleted. A revoked token is kept until it would have expired anyway:
     * while it exists, reusing it is detected as theft; once it has expired, it is useless to a thief too.
     * Running on several instances at once is harmless: the DELETE is idempotent.
     */
    @Scheduled(cron = "${app.refresh-token.cleanup-cron}")
    void deleteExpired() {
        int deleted = refreshTokenRepository.deleteExpired(clock.instant());
        log.info("Deleted {} expired refresh tokens", deleted);
    }

    private IssuedRefreshToken issue(UUID userId, UUID familyId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant expiresAt = clock.instant().plus(properties.ttl());
        refreshTokenRepository.save(new RefreshToken(userId, familyId, hash(rawToken), expiresAt));
        return new IssuedRefreshToken(rawToken, properties.ttl());
    }

    /**
     * SHA-256, not BCrypt: the token is 256 random bits, so there is nothing to brute-force and no need
     * for a slow hash; and a deterministic hash lets us find the row by it (BCrypt's random salt would not).
     */
    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required on every JVM", e);
        }
    }

    record Rotation(UUID userId, IssuedRefreshToken refreshToken) {
    }
}
