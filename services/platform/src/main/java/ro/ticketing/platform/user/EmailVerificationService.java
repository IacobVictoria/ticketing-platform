package ro.ticketing.platform.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@Transactional
class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration EXISTING_ACCOUNT_EMAIL_COOLDOWN = Duration.ofHours(1);
    private static final Duration UNVERIFIED_ACCOUNT_LIFETIME = Duration.ofDays(7);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    EmailVerificationService(EmailVerificationTokenRepository tokenRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, ApplicationEventPublisher eventPublisher, Clock clock) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Creates a new token (older ones stop working); the email leaves only after the transaction commits.
     * Does nothing if a token was created in the last minute, so nobody can flood an inbox or keep killing its links.
     * The caller holds the lock on the user's row, or the user is brand new, so these steps never run in parallel.
     */
    void sendVerificationEmail(User user) {
        Instant now = clock.instant();
        if (tokenRepository.existsByUserIdAndCreatedAtAfter(user.getId(), now.minus(RESEND_COOLDOWN))) {
            return;
        }
        tokenRepository.deleteAllByUserId(user.getId());

        String rawToken = RandomTokens.generate();
        tokenRepository.save(new EmailVerificationToken(user.getId(), RandomTokens.hash(rawToken),
                now.plus(TOKEN_VALIDITY)));

        eventPublisher.publishEvent(new VerificationEmailRequested(user.getId(), user.getEmail(), rawToken));
    }

    /**
     * Someone registered with the email of a verified account: we tell the owner instead of telling the caller.
     * At most one such email per hour, so registering in a loop cannot flood the owner's inbox.
     * The caller holds the lock on the user's row.
     */
    void sendExistingAccountEmail(User user) {
        Instant now = clock.instant();
        if (user.existingAccountEmailSentAfter(now.minus(EXISTING_ACCOUNT_EMAIL_COOLDOWN))) {
            return;
        }
        user.markExistingAccountEmailSent(now);
        eventPublisher.publishEvent(new ExistingAccountEmailRequested(user.getId(), user.getEmail()));
    }

    /**
     * The token from the link plus the account's password: marks the email as verified and deletes the token.
     * The password proves that whoever clicks is also who registered. Otherwise someone could register with
     * another person's email and a password of their own, and that person's click would activate the account.
     */
    void verify(String rawToken, String password) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(RandomTokens.hash(rawToken))
                .orElseThrow(InvalidVerificationTokenException::new);
        if (token.isExpired(clock.instant())) {
            throw new InvalidVerificationTokenException();
        }

        // SELECT ... FOR UPDATE: two clicks on the same link run one after the other
        User user = userRepository.findForUpdateById(token.getUserId())
                .orElseThrow(InvalidVerificationTokenException::new);
        if (user.isEmailVerified()) {
            throw new InvalidVerificationTokenException();
        }
        if (UserService.exceedsBcryptLimit(password) || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.markEmailVerified(clock.instant());
        tokenRepository.deleteAllByUserId(user.getId());
    }

    /**
     * Runs in the background, so the caller answers immediately and just as fast in every case: the response
     * time does not reveal whether the email has an unverified account. Unknown and verified emails get nothing.
     */
    @Async
    void resend(String email) {
        userRepository.findForUpdateByEmail(User.normalizeEmail(email))
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerificationEmail);
    }

    /** Every night at 03:30 (after the refresh token cleanup). Unverified accounts must not hold an email forever. */
    @Scheduled(cron = "0 30 3 * * *")
    void deleteExpired() {
        Instant now = clock.instant();
        int tokens = tokenRepository.deleteExpired(now);
        int accounts = userRepository.deleteUnverifiedUpdatedBefore(now.minus(UNVERIFIED_ACCOUNT_LIFETIME));
        log.info("Deleted {} expired email verification tokens and {} unverified accounts", tokens, accounts);
    }
}
