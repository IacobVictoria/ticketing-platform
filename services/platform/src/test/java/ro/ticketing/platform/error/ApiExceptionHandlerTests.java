package ro.ticketing.platform.error;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/** Plain unit test: no Spring context, the handler is created by hand. */
class ApiExceptionHandlerTests {

    ApiExceptionHandler handler = new ApiExceptionHandler(Clock.fixed(Instant.parse("2026-10-28T10:00:00Z"), ZoneOffset.UTC));

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/venues");

    @Test
    void checkViolationIsInvalidData() {
        ProblemDetail problem = handler.handleDataIntegrity(violation("23514"), request);

        assertThat(problem.getStatus()).isEqualTo(422);
        assertThat(problem.getProperties()).containsEntry("code", "DATA_INVALID");
    }

    @Test
    void notNullViolationIsInvalidData() {
        assertThat(handler.handleDataIntegrity(violation("23502"), request).getStatus()).isEqualTo(422);
    }

    @Test
    void uniqueViolationIsConflict() {
        ProblemDetail problem = handler.handleDataIntegrity(violation("23505"), request);

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getProperties()).containsEntry("code", "DATA_CONFLICT");
    }

    @Test
    void foreignKeyViolationIsConflict() {
        assertThat(handler.handleDataIntegrity(violation("23503"), request).getStatus()).isEqualTo(409);
    }

    private static DataIntegrityViolationException violation(String sqlState) {
        return new DataIntegrityViolationException("constraint violated", new SQLException("db error", sqlState));
    }
}
