package ro.ticketing.platform.error;

import java.util.Map;

/** The requested resource does not exist. Mapped to 404. */
public abstract class NotFoundException extends DomainException {

    protected NotFoundException(String code, String message, Map<String, Object> properties) {
        super(code, message, properties);
    }
}
