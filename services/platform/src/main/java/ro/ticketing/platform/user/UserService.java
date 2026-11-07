package ro.ticketing.platform.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class UserService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final Clock clock;
    /** Checked against when the email is unknown, so that case takes as long as a wrong password. */
    private final String dummyPasswordHash;

    UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-unknown-emails");
    }

    /**
     * Looks the same from outside for every email (the caller always answers 202 with no body), so nobody can use
     * it to find out who has an account. What happens differs, and the email tells the owner of the address:
     * - new email: a new account and a verification email (the user and the token are saved together);
     * - unverified email: it is not really taken yet, so the old details are replaced and a new link is sent;
     *   that way nobody can block an address by registering it first;
     * - verified email: nothing changes, the owner gets a "you already have an account" email.
     * The password is hashed in every case, so the response takes about as long whatever the email.
     * Two concurrent registrations with a new email can both find no row; the UNIQUE constraint rejects
     * the second one (409 DATA_CONFLICT), which only tells it that someone registered at the same moment.
     */
    @Transactional
    void register(String email, String password, String firstName, String lastName) {
        if (exceedsBcryptLimit(password)) {
            throw new PasswordTooLongException(BCRYPT_MAX_PASSWORD_BYTES);
        }
        String normalizedEmail = User.normalizeEmail(email);
        String passwordHash = passwordEncoder.encode(password);

        Optional<User> existing = userRepository.findForUpdateByEmail(normalizedEmail);
        if (existing.isEmpty()) {
            User user = userRepository.save(new User(normalizedEmail, passwordHash, firstName, lastName, UserRole.USER));
            // the token row references the user, so the user's row must be in the database first
            userRepository.flush();
            emailVerificationService.sendVerificationEmail(user);
        } else if (existing.get().isEmailVerified()) {
            emailVerificationService.sendExistingAccountEmail(existing.get());
        } else {
            existing.get().replaceRegistration(passwordHash, firstName, lastName);
            emailVerificationService.sendVerificationEmail(existing.get());
        }
    }

    /** Only for {@link AdminBootstrap}; there is no API that creates admins. The admin needs no email confirmation. */
    @Transactional
    User createAdmin(String email, String password, String firstName, String lastName) {
        User admin = create(email, password, firstName, lastName, UserRole.ADMIN);
        admin.markEmailVerified(clock.instant());
        return admin;
    }

    boolean adminExists() {
        return userRepository.existsByRole(UserRole.ADMIN);
    }

    boolean emailExists(String email) {
        return userRepository.existsByEmail(User.normalizeEmail(email));
    }

    private User create(String email, String password, String firstName, String lastName, UserRole role) {
        String normalizedEmail = User.normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }
        if (exceedsBcryptLimit(password)) {
            throw new PasswordTooLongException(BCRYPT_MAX_PASSWORD_BYTES);
        }
        String passwordHash = passwordEncoder.encode(password);
        return userRepository.save(new User(normalizedEmail, passwordHash, firstName, lastName, role));
    }

    /**
     * Unknown email and wrong password fail the same way, with the same message and roughly the same duration
     * (a BCrypt check runs in both cases), so an attacker cannot find out which emails have accounts.
     */
    User authenticate(String email, String password) {
        // no account can have such a password (register rejects it), and BCrypt would throw on it
        if (exceedsBcryptLimit(password)) {
            throw new InvalidCredentialsException();
        }
        Optional<User> user = userRepository.findByEmail(User.normalizeEmail(email));
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(password, hash);

        if (user.isEmpty() || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        // checked only after the password, so it reveals nothing to someone who does not know it
        if (!user.get().isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
        return user.get();
    }

    Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    static boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES;
    }
}
