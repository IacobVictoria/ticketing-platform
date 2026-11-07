package ro.ticketing.platform.error;

import java.util.Map;

/** We know who the caller is, but they may not do this (e.g. email not verified yet). Mapped to 403. */
public abstract class ForbiddenException extends DomainException {

    protected ForbiddenException(String code, String message, Map<String, Object> properties) {
        super(code, message, properties);
    }
}
