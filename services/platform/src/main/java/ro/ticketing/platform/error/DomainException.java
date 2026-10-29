package ro.ticketing.platform.error;

import java.util.Map;

/**
 * Base for business errors. Subclasses say *what kind* of error it is (not found, conflict, rule);
 * {@link ApiExceptionHandler} decides the HTTP status for each kind.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final Map<String, Object> properties;

    protected DomainException(String code, String message, Map<String, Object> properties) {
        super(message);
        this.code = code;
        this.properties = Map.copyOf(properties);
    }

    /** Stable, machine-readable code (e.g. EVENT_NOT_FOUND) the frontend can rely on. */
    public String getCode() {
        return code;
    }

    /** Extra context added to the error response (e.g. eventId, currentStatus). */
    public Map<String, Object> getProperties() {
        return properties;
    }
}
