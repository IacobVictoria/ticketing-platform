package ro.ticketing.platform.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EmailVerificationApiTests {

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("verify-email\\?token=([A-Za-z0-9_-]+)");
    /** Emails leave in the background, so the tests wait for them, up to this many milliseconds. */
    private static final int EMAIL_WAIT = 5000;
    /** How long a test waits to be sure that no other email is coming. */
    private static final int NO_EMAIL_WAIT = 500;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    EmailVerificationService emailVerificationService;

    /** A fake mail sender: nothing is sent, and we can read the email the app tried to send. */
    @MockitoBean
    JavaMailSender mailSender;

    AuthApi api;
    String email;

    @BeforeEach
    void registerWithoutConfirming() {
        api = new AuthApi(mvc, jdbcClient);
        email = uniqueEmail();
        assertThat(register(email, AuthApi.PASSWORD)).hasStatus(HttpStatus.ACCEPTED);
    }

    @Test
    void registerSendsAnEmailWithTheLink() {
        SimpleMailMessage message = waitForEmail(1);

        assertThat(message.getTo()).containsExactly(email);
        assertThat(message.getText()).contains("http://localhost:4200/verify-email?token=");
    }

    @Test
    void registerSucceedsEvenIfTheMailServerIsDown() {
        doThrow(new MailSendException("mail server is down")).when(mailSender).send(any(SimpleMailMessage.class));
        String otherEmail = uniqueEmail();

        assertThat(register(otherEmail, AuthApi.PASSWORD)).hasStatus(HttpStatus.ACCEPTED);
        assertThat(tokenCountOf(otherEmail)).isEqualTo(1);
    }

    @Test
    void cannotLogInBeforeConfirming() {
        MvcTestResult result = api.login(email, AuthApi.PASSWORD);

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("EMAIL_NOT_VERIFIED");
    }

    @Test
    void wrongPasswordStillSaysInvalidCredentialsForUnconfirmedAccounts() {
        // the "not verified" answer is given only to someone who knows the password
        assertThat(api.login(email, "parola-gresita")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void verifyingTheTokenAllowsLogin() {
        String token = tokenFrom(waitForEmail(1));

        assertThat(verifyEmail(token, AuthApi.PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(api.login(email, AuthApi.PASSWORD)).hasStatusOk();
    }

    @Test
    void clickingTheLinkWithoutThePasswordOfTheAccountActivatesNothing() {
        // someone else registered with this email: the owner gets the link, but does not know their password
        String token = tokenFrom(waitForEmail(1));

        MvcTestResult result = verifyEmail(token, "parola-proprietarului");

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_CREDENTIALS");
        assertThat(api.login(email, AuthApi.PASSWORD)).hasStatus(HttpStatus.FORBIDDEN);
        // the link still works for whoever knows the password
        assertThat(verifyEmail(token, AuthApi.PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void theRealOwnerCanTakeOverAnUnconfirmedRegistration() {
        waitForEmail(1);
        allowResendNow(email);

        assertThat(register(email, "parola-proprietarului")).hasStatus(HttpStatus.ACCEPTED);
        String token = tokenFrom(waitForEmail(2));

        assertThat(verifyEmail(token, AuthApi.PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(verifyEmail(token, "parola-proprietarului")).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(api.login(email, "parola-proprietarului")).hasStatusOk();
        assertThat(api.login(email, AuthApi.PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenWorksOnlyOnce() {
        String token = tokenFrom(waitForEmail(1));
        verifyEmail(token, AuthApi.PASSWORD);

        MvcTestResult second = verifyEmail(token, AuthApi.PASSWORD);

        assertThat(second).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(second).bodyJson().extractingPath("$.code").isEqualTo("INVALID_VERIFICATION_TOKEN");
    }

    @Test
    void twoSimultaneousClicksConfirmOnceWithoutAServerError() {
        String token = tokenFrom(waitForEmail(1));

        CompletableFuture<MvcTestResult> first = CompletableFuture.supplyAsync(() -> verifyEmail(token, AuthApi.PASSWORD));
        CompletableFuture<MvcTestResult> second = CompletableFuture.supplyAsync(() -> verifyEmail(token, AuthApi.PASSWORD));

        List<Integer> statuses = List.of(first.join().getResponse().getStatus(), second.join().getResponse().getStatus());
        assertThat(statuses).containsExactlyInAnyOrder(204, 422);
        assertThat(api.login(email, AuthApi.PASSWORD)).hasStatusOk();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = tokenFrom(waitForEmail(1));
        jdbcClient.sql("update email_verification_tokens set expires_at = now() - interval '1 second' "
                        + "where token_hash = :hash")
                .param("hash", RandomTokens.hash(token))
                .update();

        assertThat(verifyEmail(token, AuthApi.PASSWORD)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void madeUpTokenIsRejected() {
        assertThat(verifyEmail("not-a-real-token", AuthApi.PASSWORD)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void resendGivesANewTokenAndTheOldOneStopsWorking() {
        String oldToken = tokenFrom(waitForEmail(1));
        allowResendNow(email);

        assertThat(resend(email)).hasStatus(HttpStatus.ACCEPTED);
        String newToken = tokenFrom(waitForEmail(2));

        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(verifyEmail(oldToken, AuthApi.PASSWORD)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(verifyEmail(newToken, AuthApi.PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void resendWithinTheCooldownSendsNothingAndTheLinkKeepsWorking() {
        String token = tokenFrom(waitForEmail(1));

        assertThat(resend(email)).hasStatus(HttpStatus.ACCEPTED);

        verify(mailSender, after(NO_EMAIL_WAIT).times(1)).send(any(SimpleMailMessage.class));
        assertThat(verifyEmail(token, AuthApi.PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void twoSimultaneousResendsCreateOneTokenAndOneEmail() {
        waitForEmail(1);
        allowResendNow(email);

        CompletableFuture<MvcTestResult> first = CompletableFuture.supplyAsync(() -> resend(email));
        CompletableFuture<MvcTestResult> second = CompletableFuture.supplyAsync(() -> resend(email));

        assertThat(first.join()).hasStatus(HttpStatus.ACCEPTED);
        assertThat(second.join()).hasStatus(HttpStatus.ACCEPTED);
        String token = tokenFrom(waitForEmail(2));
        verify(mailSender, after(NO_EMAIL_WAIT).times(2)).send(any(SimpleMailMessage.class));
        assertThat(tokenCountOf(email)).isEqualTo(1);
        assertThat(verifyEmail(token, AuthApi.PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void registeringAVerifiedEmailTellsTheOwnerByEmailAtMostOncePerHour() {
        String token = tokenFrom(waitForEmail(1));
        verifyEmail(token, AuthApi.PASSWORD);

        assertThat(register(email, "parola-atacatorului")).hasStatus(HttpStatus.ACCEPTED);
        SimpleMailMessage notice = waitForEmail(2);
        assertThat(notice.getTo()).containsExactly(email);
        assertThat(notice.getSubject()).isEqualTo("Ai deja un cont");
        assertThat(notice.getText()).contains("http://localhost:4200/login").doesNotContain("token=");

        assertThat(register(email, "parola-atacatorului")).hasStatus(HttpStatus.ACCEPTED);
        verify(mailSender, after(NO_EMAIL_WAIT).times(2)).send(any(SimpleMailMessage.class));
        assertThat(api.login(email, AuthApi.PASSWORD)).hasStatusOk();
        assertThat(api.login(email, "parola-atacatorului")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resendForUnknownEmailLooksTheSameButSendsNothing() {
        waitForEmail(1);

        assertThat(resend(uniqueEmail())).hasStatus(HttpStatus.ACCEPTED);

        verify(mailSender, after(NO_EMAIL_WAIT).times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void cleanupDeletesExpiredTokensAndOldUnverifiedAccounts() {
        waitForEmail(1);
        String recentUnverified = email;
        String oldUnverified = uniqueEmail();
        String oldVerified = uniqueEmail();
        String expiredTokenOwner = uniqueEmail();
        register(oldUnverified, AuthApi.PASSWORD);
        register(oldVerified, AuthApi.PASSWORD);
        register(expiredTokenOwner, AuthApi.PASSWORD);
        AuthApi.markEmailVerified(jdbcClient, oldVerified);
        makeAccountOlderThanAWeek(oldUnverified);
        makeAccountOlderThanAWeek(oldVerified);
        jdbcClient.sql("""
                        update email_verification_tokens set expires_at = now() - interval '1 second'
                        where user_id = (select id from users where email = :email)
                        """)
                .param("email", expiredTokenOwner)
                .update();

        emailVerificationService.deleteExpired();

        assertThat(accountExists(oldUnverified)).isFalse();
        assertThat(accountExists(oldVerified)).isTrue();
        assertThat(accountExists(recentUnverified)).isTrue();
        assertThat(tokenCountOf(recentUnverified)).isEqualTo(1);
        assertThat(tokenCountOf(expiredTokenOwner)).isZero();
    }

    private MvcTestResult register(String email, String password) {
        return api.post("/api/auth/register", """
                {"email": "%s", "password": "%s", "confirmPassword": "%s",
                 "firstName": "Ana", "lastName": "Pop"}
                """.formatted(email, password, password));
    }

    private MvcTestResult verifyEmail(String token, String password) {
        return api.post("/api/auth/verify-email", """
                {"token": "%s", "password": "%s"}
                """.formatted(token, password));
    }

    private MvcTestResult resend(String email) {
        return api.post("/api/auth/resend-verification", """
                {"email": "%s"}
                """.formatted(email));
    }

    /** Waits until the app has sent this many emails since the test started and returns the last one. */
    private SimpleMailMessage waitForEmail(int count) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(EMAIL_WAIT).times(count)).send(captor.capture());
        return captor.getValue();
    }

    /** As if the last token was created more than a minute ago. */
    private void allowResendNow(String email) {
        jdbcClient.sql("""
                        update email_verification_tokens set created_at = now() - interval '2 minutes'
                        where user_id = (select id from users where email = :email)
                        """)
                .param("email", email)
                .update();
    }

    private void makeAccountOlderThanAWeek(String email) {
        jdbcClient.sql("update users set updated_at = now() - interval '8 days' where email = :email")
                .param("email", email)
                .update();
    }

    private boolean accountExists(String email) {
        return jdbcClient.sql("select count(*) from users where email = :email")
                .param("email", email)
                .query(Long.class)
                .single() == 1;
    }

    private long tokenCountOf(String email) {
        return jdbcClient.sql("""
                        select count(*) from email_verification_tokens
                        where user_id = (select id from users where email = :email)
                        """)
                .param("email", email)
                .query(Long.class)
                .single();
    }

    private static String uniqueEmail() {
        return "ana." + UUID.randomUUID() + "@example.ro";
    }

    private static String tokenFrom(SimpleMailMessage message) {
        Matcher matcher = TOKEN_IN_LINK.matcher(message.getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
