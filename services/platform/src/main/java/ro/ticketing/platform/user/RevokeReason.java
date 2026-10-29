package ro.ticketing.platform.user;

enum RevokeReason {
    /** Exchanged for a new token at /refresh (normal use). */
    ROTATED,
    /** The user logged out. */
    LOGOUT,
    /** A rotated token was used again: someone else may have a copy, so the whole family was revoked. */
    REUSE_DETECTED
}
