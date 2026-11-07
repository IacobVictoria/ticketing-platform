package ro.ticketing.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ro.ticketing.platform.common.BaseEntity;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "users")
class User extends BaseEntity {

    @Column(nullable = false, length = 254)
    private String email;

    /** BCrypt hash; the plain password is never stored. */
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** Null until the user clicks the link in the verification email. */
    private Instant emailVerifiedAt;

    /** Last "someone tried to register with your email" message; null if never sent. */
    private Instant existingAccountEmailSentAt;

    protected User() {
    }

    User(String email, String passwordHash, String firstName, String lastName, UserRole role) {
        this.email = normalizeEmail(email);
        this.passwordHash = passwordHash;
        this.firstName = firstName.strip();
        this.lastName = lastName.strip();
        this.role = role;
    }

    /** Emails are stored trimmed and lower-case, so "Ana@X.ro " and "ana@x.ro" are the same account. */
    static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    void markEmailVerified(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
        }
    }

    /** Registering again with an unverified email: the newest registration wins. */
    void replaceRegistration(String passwordHash, String firstName, String lastName) {
        this.passwordHash = passwordHash;
        this.firstName = firstName.strip();
        this.lastName = lastName.strip();
    }

    boolean existingAccountEmailSentAfter(Instant time) {
        return existingAccountEmailSentAt != null && existingAccountEmailSentAt.isAfter(time);
    }

    void markExistingAccountEmailSent(Instant now) {
        existingAccountEmailSentAt = now;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    String getFirstName() {
        return firstName;
    }

    String getLastName() {
        return lastName;
    }

    UserRole getRole() {
        return role;
    }
}
