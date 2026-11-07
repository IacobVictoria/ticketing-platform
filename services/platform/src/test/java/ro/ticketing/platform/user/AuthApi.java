package ro.ticketing.platform.user;

import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Calls to /api/auth shared by the auth tests. */
class AuthApi {

    static final String PASSWORD = "parola-sigura";

    private final MockMvcTester mvc;
    private final JdbcClient jdbcClient;

    AuthApi(MockMvcTester mvc, JdbcClient jdbcClient) {
        this.mvc = mvc;
        this.jdbcClient = jdbcClient;
    }

    /** Registers a new user with a unique email, confirms the email directly in the database and returns it. */
    String registerNewUser() {
        String email = "ana." + UUID.randomUUID() + "@example.ro";
        MvcTestResult result = post("/api/auth/register", """
                {"email": "%s", "password": "%s", "confirmPassword": "%s", "firstName": "Ana", "lastName": "Pop"}
                """.formatted(email, PASSWORD, PASSWORD));
        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
        markEmailVerified(jdbcClient, email);
        return email;
    }

    /** Shortcut for tests that are not about email verification: as if the user clicked the link. */
    static void markEmailVerified(JdbcClient jdbcClient, String email) {
        jdbcClient.sql("update users set email_verified_at = now() where email = :email")
                .param("email", User.normalizeEmail(email))
                .update();
    }

    MvcTestResult login(String email, String password) {
        return post("/api/auth/login", """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password));
    }

    /** Logs in and returns the refresh token from the Set-Cookie header. */
    String loginAndGetRefreshToken(String email) {
        MvcTestResult result = login(email, PASSWORD);
        assertThat(result).hasStatusOk();
        return refreshTokenOf(result);
    }

    MvcTestResult refresh(String refreshToken) {
        return mvc.post().uri("/api/auth/refresh").cookie(new Cookie(RefreshTokenCookies.NAME, refreshToken)).exchange();
    }

    MvcTestResult logout(String refreshToken) {
        return mvc.post().uri("/api/auth/logout").cookie(new Cookie(RefreshTokenCookies.NAME, refreshToken)).exchange();
    }

    MvcTestResult post(String uri, String json) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    /** "refresh_token=abc; Path=/api/auth; ..." → "abc" */
    static String refreshTokenOf(MvcTestResult result) {
        String header = setCookieOf(result);
        return header.substring((RefreshTokenCookies.NAME + "=").length(), header.indexOf(';'));
    }

    static String setCookieOf(MvcTestResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).startsWith(RefreshTokenCookies.NAME + "=");
        return header;
    }
}
