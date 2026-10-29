package ro.ticketing.platform.user;

public enum UserRole {
    /** Buys tickets. Every account created through register gets this role. */
    USER,
    /** Manages events and venues. Never assigned from a request. */
    ADMIN
}
