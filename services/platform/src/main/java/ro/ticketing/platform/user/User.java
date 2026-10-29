package ro.ticketing.platform.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ro.ticketing.platform.common.BaseEntity;

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
