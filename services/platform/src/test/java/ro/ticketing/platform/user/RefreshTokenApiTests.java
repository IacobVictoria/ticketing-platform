package ro.ticketing.platform.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import ro.ticketing.platform.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenApiTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbcClient;

    AuthApi api;
    String email;

    @BeforeEach
    void registerUser() {
        api = new AuthApi(mvc);
        email = api.registerNewUser();
    }

    @Test
    void loginSetsHardenedRefreshCookie() {
        MvcTestResult result = api.login(email, AuthApi.PASSWORD);

        String cookie = AuthApi.setCookieOf(result);
        assertThat(cookie).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/auth", "Max-Age=604800");
        // the refresh token is only in the cookie, never in the JSON body
        assertThat(result).bodyJson().doesNotHavePath("$.refreshToken");
        assertThat(result).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
    }

    @Test
    void onlyTheHashOfTheTokenIsStored() {
        String refreshToken = api.loginAndGetRefreshToken(email);

        String storedHash = jdbcClient.sql("select token_hash from refresh_tokens where token_hash = :hash")
                .param("hash", RefreshTokenService.hash(refreshToken))
                .query(String.class)
                .single();
        assertThat(storedHash).isNotEqualTo(refreshToken).hasSize(64);
        assertThat(jdbcClient.sql("select count(*) from refresh_tokens where token_hash = :raw")
                .param("raw", refreshToken).query(Long.class).single()).isZero();
    }

    @Test
    void refreshReturnsNewAccessTokenAndRotatesTheCookie() {
        String oldToken = api.loginAndGetRefreshToken(email);

        MvcTestResult result = api.refresh(oldToken);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.tokenType").isEqualTo("Bearer");
        assertThat(result).bodyJson().extractingPath("$.user.email").isEqualTo(email);
        String newToken = AuthApi.refreshTokenOf(result);
        assertThat(newToken).isNotEqualTo(oldToken);

        // the new token works, and can itself be rotated
        assertThat(api.refresh(newToken)).hasStatusOk();
    }

    @Test
    void concurrentRefreshFromAnotherTabDoesNotLogTheUserOut() {
        String oldToken = api.loginAndGetRefreshToken(email);
        String newToken = AuthApi.refreshTokenOf(api.refresh(oldToken));

        // a second tab still had the old cookie: refused, but within the grace period, so no family revocation
        assertThat(api.refresh(oldToken)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(api.refresh(newToken)).hasStatusOk();
    }

    @Test
    void refreshWithoutCookieIsRejected() {
        MvcTestResult result = mvc.post().uri("/api/auth/refresh").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void unknownTokenIsRejected() {
        assertThat(api.refresh("not-a-real-token")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void expiredTokenIsRejected() {
        String refreshToken = api.loginAndGetRefreshToken(email);
        jdbcClient.sql("update refresh_tokens set expires_at = now() - interval '1 second' where token_hash = :hash")
                .param("hash", RefreshTokenService.hash(refreshToken))
                .update();

        assertThat(api.refresh(refreshToken)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesTheTokenAndClearsTheCookie() {
        String refreshToken = api.loginAndGetRefreshToken(email);

        MvcTestResult result = api.logout(refreshToken);

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(AuthApi.setCookieOf(result)).contains("Max-Age=0", "Path=/api/auth");
        assertThat(api.refresh(refreshToken)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutIsIdempotent() {
        String refreshToken = api.loginAndGetRefreshToken(email);

        assertThat(api.logout(refreshToken)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(api.logout(refreshToken)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.post().uri("/api/auth/logout").exchange()).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void eachLoginIsASeparateSession() {
        String laptop = api.loginAndGetRefreshToken(email);
        String phone = api.loginAndGetRefreshToken(email);

        api.logout(laptop);

        assertThat(api.refresh(phone)).hasStatusOk();
        assertThat(familyOf(laptop)).isNotEqualTo(familyOf(phone));
    }

    private String familyOf(String refreshToken) {
        return jdbcClient.sql("select family_id::text from refresh_tokens where token_hash = :hash")
                .param("hash", RefreshTokenService.hash(refreshToken))
                .query(String.class)
                .single();
    }
}
