package ro.ticketing.platform.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Random secret tokens (refresh tokens, email verification links) and their hashes. */
final class RandomTokens {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RandomTokens() {
    }

    /** 32 random bytes (256 bits), URL-safe so it can go into a cookie or a link: impossible to guess. */
    static String generate() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
}
