package ro.ticketing.platform.user;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class LoginApiTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtDecoder jwtDecoder;

    String email;
    String userId;

    @BeforeEach
    void registerUser() {
        email = "ana." + UUID.randomUUID() + "@example.ro";
        MvcTestResult result = post("/api/auth/register", """
                {"email": "%s", "password": "parola-sigura", "confirmPassword": "parola-sigura",
                 "firstName": "Ana", "lastName": "Pop"}
                """.formatted(email));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        userId = JsonPath.read(bodyOf(result), "$.id");
    }

    @Test
    void loginReturnsSignedTokenAndUser() {
        MvcTestResult result = login(email, "parola-sigura");

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
        assertThat(result).bodyJson().extractingPath("$.expiresIn").isEqualTo(3600);
        assertThat(result).bodyJson().extractingPath("$.user.id").isEqualTo(userId);
        assertThat(result).bodyJson().extractingPath("$.user.role").isEqualTo("USER");

        Jwt jwt = jwtDecoder.decode(tokenOf(result));
        assertThat(jwt.getSubject()).isEqualTo(userId);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("ticketing-platform");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(1));
        assertThat(jwt.getClaims()).doesNotContainKeys("email", "password", "passwordHash");
    }

    @Test
    void emailIsCaseInsensitiveAndTrimmed() {
        assertThat(login("  " + email.toUpperCase() + " ", "parola-sigura")).hasStatusOk();
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameResponse() {
        MvcTestResult wrongPassword = login(email, "parola-gresita");
        MvcTestResult unknownEmail = login("nobody." + UUID.randomUUID() + "@example.ro", "parola-sigura");

        for (MvcTestResult result : new MvcTestResult[] {wrongPassword, unknownEmail}) {
            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_CREDENTIALS");
            assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Invalid email or password");
        }
    }

    @Test
    void passwordOverBcryptByteLimitIsInvalidCredentialsNotServerError() {
        assertThat(login(email, "ă".repeat(40))).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void missingFieldsAreValidationErrors() {
        MvcTestResult result = post("/api/auth/login", """
                {"email": "", "password": null}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[*].field").asArray()
                .containsExactlyInAnyOrder("email", "password");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = tokenOf(login(email, "parola-sigura"));
        String[] parts = token.split("\\.");
        // swap the payload for one claiming ADMIN, keep the original signature
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                """
                {"sub":"%s","role":"ADMIN","iss":"ticketing-platform","exp":4102444800}
                """.formatted(userId).strip().getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtDecoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    private MvcTestResult login(String email, String password) {
        return post("/api/auth/login", """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
    }

    private MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String tokenOf(MvcTestResult result) {
        return JsonPath.read(bodyOf(result), "$.accessToken");
    }

    private static String bodyOf(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
