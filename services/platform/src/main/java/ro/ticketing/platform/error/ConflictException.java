package ro.ticketing.platform.error;

import java.util.Map;

/** The request is valid but clashes with the current state of the resource. Mapped to 409. */
public abstract class ConflictException extends DomainException {

    protected ConflictException(String code, String message, Map<String, Object> properties) {
        super(code, message, properties);
    }
}
