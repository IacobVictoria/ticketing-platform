package ro.ticketing.platform.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import ro.ticketing.platform.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/** Grace period 0: any reuse of a rotated token counts as theft. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.refresh-token.reuse-grace=0s")
@AutoConfigureMockMvc
class RefreshTokenReuseTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void reusingARotatedTokenRevokesTheWholeFamily() {
        AuthApi api = new AuthApi(mvc);
        String email = api.registerNewUser();
        String stolen = api.loginAndGetRefreshToken(email);

        // the real user refreshes and keeps using the new token...
        String legit = AuthApi.refreshTokenOf(api.refresh(stolen));
        // ...then the thief uses the copy of the old one
        assertThat(api.refresh(stolen)).hasStatus(HttpStatus.UNAUTHORIZED);

        // neither of them can continue: both must log in again
        assertThat(api.refresh(legit)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(jdbcClient.sql("select revoke_reason from refresh_tokens where token_hash = :hash")
                .param("hash", RefreshTokenService.hash(legit))
                .query(String.class)
                .single()).isEqualTo("REUSE_DETECTED");
    }
}
