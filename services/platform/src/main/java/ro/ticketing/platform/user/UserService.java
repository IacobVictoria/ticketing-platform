package ro.ticketing.platform.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
class UserService {

    private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    /** Checked against when the email is unknown, so that case takes as long as a wrong password. */
    private final String dummyPasswordHash;

    UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-unknown-emails");
    }

    /**
     * Two concurrent registrations with the same email can both pass the exists check;
     * the UNIQUE constraint rejects the second one (409 DATA_CONFLICT).
     */
    @Transactional
    User register(String email, String password, String firstName, String lastName) {
        String normalizedEmail = User.normalizeEmail(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }
        if (exceedsBcryptLimit(password)) {
            throw new PasswordTooLongException(BCRYPT_MAX_PASSWORD_BYTES);
        }
        String passwordHash = passwordEncoder.encode(password);
        return userRepository.save(new User(normalizedEmail, passwordHash, firstName, lastName, UserRole.USER));
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
        return user.get();
    }

    Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    private static boolean exceedsBcryptLimit(String password) {
        return password.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES;
    }
}
