package ro.ticketing.platform.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import ro.ticketing.platform.TestcontainersConfiguration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The database must reject invalid users even when they bypass the Java code (e.g. a manual INSERT). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserSchemaConstraintsTests {

    @Autowired
    JdbcClient jdbcClient;

    @Test
    void validUserIsAccepted() {
        assertThatCode(() -> insertUser(uniqueEmail(), "Ana", "USER")).doesNotThrowAnyException();
    }

    @Test
    void duplicateEmailIsRejected() {
        String email = uniqueEmail();
        insertUser(email, "Ana", "USER");

        assertThatThrownBy(() -> insertUser(email, "Ana", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_email_unique");
    }

    @Test
    void notNormalizedEmailIsRejected() {
        assertThatThrownBy(() -> insertUser("Ana." + UUID.randomUUID() + "@example.ro", "Ana", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_email_normalized_check");
        assertThatThrownBy(() -> insertUser(" " + uniqueEmail(), "Ana", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_email_normalized_check");
    }

    @Test
    void blankOrUntrimmedNameIsRejected() {
        assertThatThrownBy(() -> insertUser(uniqueEmail(), "", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_names_trimmed_check");
        assertThatThrownBy(() -> insertUser(uniqueEmail(), " Ana", "USER"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_names_trimmed_check");
    }

    @Test
    void unknownRoleIsRejected() {
        assertThatThrownBy(() -> insertUser(uniqueEmail(), "Ana", "SUPERADMIN"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("users_role_check");
    }

    private static String uniqueEmail() {
        return "user." + UUID.randomUUID() + "@example.ro";
    }

    private void insertUser(String email, String firstName, String role) {
        jdbcClient.sql("""
                        insert into users (id, email, password_hash, first_name, last_name, role, created_at, updated_at)
                        values (:id, :email, 'hash', :firstName, 'Pop', :role, now(), now())
                        """)
                .param("id", UUID.randomUUID())
                .param("email", email)
                .param("firstName", firstName)
                .param("role", role)
                .update();
    }
}
