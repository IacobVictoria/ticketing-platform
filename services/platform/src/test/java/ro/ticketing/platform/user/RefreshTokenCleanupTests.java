package ro.ticketing.platform.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import ro.ticketing.platform.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenCleanupTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbcClient;

    @Autowired
    RefreshTokenService refreshTokenService;

    @Test
    void deletesExpiredTokensButKeepsRevokedOnesUntilTheyExpire() {
        AuthApi api = new AuthApi(mvc);
        String email = api.registerNewUser();
        String expired = api.loginAndGetRefreshToken(email);
        String active = api.loginAndGetRefreshToken(email);
        String loggedOut = api.loginAndGetRefreshToken(email);
        api.logout(loggedOut);
        jdbcClient.sql("update refresh_tokens set expires_at = now() - interval '1 second' where token_hash = :hash")
                .param("hash", RefreshTokenService.hash(expired))
                .update();

        refreshTokenService.deleteExpired();

        assertThat(exists(expired)).isFalse();
        assertThat(exists(active)).isTrue();
        // still needed: reusing it must be recognized until it expires
        assertThat(exists(loggedOut)).isTrue();
    }

    private boolean exists(String refreshToken) {
        return jdbcClient.sql("select count(*) from refresh_tokens where token_hash = :hash")
                .param("hash", RefreshTokenService.hash(refreshToken))
                .query(Long.class)
                .single() == 1;
    }
}
