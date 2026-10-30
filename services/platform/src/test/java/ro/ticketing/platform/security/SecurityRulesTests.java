package ro.ticketing.platform.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real tokens end to end: issued by /api/auth/login, verified by the resource server filter.
 * The admin is the one AdminBootstrap creates from the default app.admin.* values.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTests {

    static final String ADMIN_EMAIL = "admin@ticketing.local";
    static final String ADMIN_PASSWORD = "admin-local-password";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JwtEncoder jwtEncoder;

    String adminToken;
    String userToken;
    String userEmail;

    @BeforeEach
    void logIn() {
        adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        userEmail = "ana." + UUID.randomUUID() + "@example.ro";
        assertThat(postJson("/api/auth/register", null, """
                {"email": "%s", "password": "parola-sigura", "confirmPassword": "parola-sigura",
                 "firstName": "Ana", "lastName": "Pop"}
                """.formatted(userEmail))).hasStatus(HttpStatus.CREATED);
        userToken = accessToken(userEmail, "parola-sigura");
    }

    @Test
    void anonymousCanBrowseEventsAndVenues() {
        assertThat(mvc.get().uri("/api/events")).hasStatusOk();
        assertThat(mvc.get().uri("/api/venues")).hasStatusOk();
    }

    @Test
    void anonymousCannotWrite() {
        assertThat(createVenue(null)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void userCannotDoAdminWork() {
        assertThat(createVenue(userToken)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanDoAdminWork() {
        assertThat(createVenue(adminToken)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void meReturnsTheCurrentUser() {
        MvcTestResult result = mvc.get().uri("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken)
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.email").isEqualTo(userEmail);
        assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("USER");
    }

    @Test
    void meRequiresAToken() {
        assertThat(mvc.get().uri("/api/auth/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void garbageTokenIsRejectedEvenOnPublicRoutes() {
        // a broken token is an error, not "anonymous": the frontend should refresh, not silently lose the login
        assertThat(mvc.get().uri("/api/events").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredTokenIsRejected() {
        Instant past = Instant.now().minus(Duration.ofHours(2));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ticketing-platform")
                .subject(UUID.randomUUID().toString())
                .issuedAt(past)
                .expiresAt(past.plus(Duration.ofMinutes(15)))
                .claim(TokenService.ROLE_CLAIM, "ADMIN")
                .build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                claims)).getTokenValue();

        assertThat(createVenue(expired)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void draftEventsAreVisibleOnlyToAdmins() {
        String eventId = JsonPath.read(bodyOf(postJson("/api/events", adminToken, """
                {"name": "Concert secret", "startsAt": "%s"}
                """.formatted(Instant.now().plus(Duration.ofDays(30))))), "$.id");

        assertThat(mvc.get().uri("/api/events/{id}", eventId)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/events/{id}", eventId).header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/events/{id}", eventId).header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .hasStatusOk();
    }

    private MvcTestResult createVenue(String token) {
        return postJson("/api/venues", token, """
                {"name": "Sala Palatului", "city": "Bucuresti"}
                """);
    }

    private String accessToken(String email, String password) {
        MvcTestResult result = postJson("/api/auth/login", null, """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
        assertThat(result).hasStatusOk();
        return JsonPath.read(bodyOf(result), "$.accessToken");
    }

    private MvcTestResult postJson(String uri, String token, String json) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return request.exchange();
    }

    private static String bodyOf(MvcTestResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
