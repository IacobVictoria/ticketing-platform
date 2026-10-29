package ro.ticketing.platform.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Turns every exception into an RFC 9457 problem response with the same shape:
 * {@code type, title, status, detail, instance, code, timestamp} (+ extra properties).
 * <p>
 * Spring MVC's own errors (malformed JSON, invalid path params, unknown URL, validation...)
 * are handled by {@link ResponseEntityExceptionHandler}; we only enrich them.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** PostgreSQL SQLSTATEs: not_null_violation, check_violation, string_data_right_truncation. */
    private static final Set<String> INVALID_DATA_STATES = Set.of("23502", "23514", "22001");

    private final Clock clock;

    ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return domainProblem(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return domainProblem(HttpStatus.UNAUTHORIZED, ex, request);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
        return domainProblem(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return domainProblem(HttpStatus.UNPROCESSABLE_CONTENT, ex, request);
    }

    /**
     * A database constraint rejected the write. Normally our services check first, but two concurrent
     * requests can both pass the check, or a code path can miss one; the database is the final guard.
     * UNIQUE / FK violations clash with existing data (409); CHECK / NOT NULL / too long mean invalid data (422).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation on {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());

        if (ex.getMostSpecificCause() instanceof SQLException sql && INVALID_DATA_STATES.contains(sql.getSQLState())) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
                    "The request contains data the database does not accept");
            return enrich(problem, "DATA_INVALID", request.getRequestURI());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request conflicts with existing data");
        return enrich(problem, "DATA_CONFLICT", request.getRequestURI());
    }

    /** Last resort: log everything, reveal nothing except an id to correlate with the logs. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error [errorId={}] on {} {}", errorId, request.getMethod(), request.getRequestURI(), ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Contact support with the errorId.");
        problem.setProperty("errorId", errorId);
        return enrich(problem, "INTERNAL_ERROR", request.getRequestURI());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = ex.getBody();
        problem.setDetail("Request validation failed");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /**
     * Final step for every Spring MVC error, after the ProblemDetail body has been built
     * (for some errors it is only created inside handleExceptionInternal), so they all get the same extra fields.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            enrich(problem, status != null ? status.name() : "HTTP_" + statusCode.value(), null);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ProblemDetail domainProblem(HttpStatus status, DomainException ex, HttpServletRequest request) {
        log.debug("{} {} -> {} {}: {}", request.getMethod(), request.getRequestURI(), status.value(), ex.getCode(),
                ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        ex.getProperties().forEach(problem::setProperty);
        return enrich(problem, ex.getCode(), request.getRequestURI());
    }

    private ProblemDetail enrich(ProblemDetail problem, String defaultCode, String path) {
        if (problem.getProperties() == null || !problem.getProperties().containsKey("code")) {
            problem.setProperty("code", defaultCode);
        }
        if (problem.getInstance() == null && path != null) {
            problem.setInstance(URI.create(path));
        }
        problem.setProperty("timestamp", clock.instant());
        return problem;
    }

    record FieldViolation(String field, String message) {
    }
}
