package ro.ticketing.platform.error;

import java.util.Map;

/** The caller could not prove who they are (e.g. wrong email or password). Mapped to 401. */
public abstract class UnauthorizedException extends DomainException {

    protected UnauthorizedException(String code, String message, Map<String, Object> properties) {
        super(code, message, properties);
    }
}
