package ro.ticketing.platform.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RegisterApiTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void registersUserWithHashedPassword() {
        String email = uniqueEmail();

        MvcTestResult result = register("""
                {"email": " %s ", "password": "parola-sigura", "confirmPassword": "parola-sigura",
                 "firstName": " Ana ", "lastName": "Pop"}
                """.formatted(email.toUpperCase()));

        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        assertThat(result).bodyText().isEmpty();

        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Ana");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.isEmailVerified()).isFalse();
        assertThat(saved.getPasswordHash()).startsWith("{bcrypt}").doesNotContain("parola-sigura");
        assertThat(passwordEncoder.matches("parola-sigura", saved.getPasswordHash())).isTrue();
    }

    @Test
    void roleFromRequestIsIgnored() {
        String email = uniqueEmail();

        MvcTestResult result = register("""
                {"email": "%s", "password": "parola-sigura", "confirmPassword": "parola-sigura",
                 "firstName": "Ana", "lastName": "Pop", "role": "ADMIN"}
                """.formatted(email));

        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        assertThat(userRepository.findByEmail(email).orElseThrow().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void registeringAVerifiedEmailLooksTheSameButChangesNothing() {
        String email = uniqueEmail();
        assertThat(register(validBody(email))).hasStatus(HttpStatus.ACCEPTED);
        AuthApi.markEmailVerified(jdbcClient, email);

        MvcTestResult result = register("""
                {"email": "%s", "password": "parola-atacatorului", "confirmPassword": "parola-atacatorului",
                 "firstName": "Mallory", "lastName": "X"}
                """.formatted(email.toUpperCase()));

        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        assertThat(result).bodyText().isEmpty();
        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Ana");
        assertThat(passwordEncoder.matches("parola-sigura", saved.getPasswordHash())).isTrue();
    }

    @Test
    void registeringAnUnverifiedEmailAgainReplacesTheOldDetails() {
        String email = uniqueEmail();
        assertThat(register(validBody(email))).hasStatus(HttpStatus.ACCEPTED);

        MvcTestResult result = register("""
                {"email": "%s", "password": "alta-parola-sigura", "confirmPassword": "alta-parola-sigura",
                 "firstName": "Maria", "lastName": "Ionescu"}
                """.formatted(email));

        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        User saved = userRepository.findByEmail(email).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Maria");
        assertThat(passwordEncoder.matches("alta-parola-sigura", saved.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("parola-sigura", saved.getPasswordHash())).isFalse();
    }

    @Test
    void mismatchedPasswordsAreRejected() {
        MvcTestResult result = register("""
                {"email": "%s", "password": "parola-sigura", "confirmPassword": "alta-parola",
                 "firstName": "Ana", "lastName": "Pop"}
                """.formatted(uniqueEmail()));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray()
                .containsExactly("passwordConfirmed");
    }

    @Test
    void invalidFieldsAreReported() {
        MvcTestResult result = register("""
                {"email": "not-an-email", "password": "scurta", "confirmPassword": "scurta",
                 "firstName": "", "lastName": "Pop"}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray()
                .containsExactlyInAnyOrder("email", "password", "firstName");
    }

    @Test
    void passwordOverBcryptByteLimitIsRejected() {
        // 40 characters, but "ă" takes 2 bytes in UTF-8: 80 bytes
        String password = "ă".repeat(40);

        MvcTestResult result = register("""
                {"email": "%s", "password": "%s", "confirmPassword": "%s", "firstName": "Ana", "lastName": "Pop"}
                """.formatted(uniqueEmail(), password, password));

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("PASSWORD_TOO_LONG");
    }

    private MvcTestResult register(String json) {
        return mvc.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String validBody(String email) {
        return """
                {"email": "%s", "password": "parola-sigura", "confirmPassword": "parola-sigura",
                 "firstName": "Ana", "lastName": "Pop"}
                """.formatted(email);
    }

    private static String uniqueEmail() {
        return "ana." + UUID.randomUUID() + "@example.ro";
    }
}
