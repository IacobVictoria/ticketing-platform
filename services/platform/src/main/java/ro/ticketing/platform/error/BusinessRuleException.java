package ro.ticketing.platform.error;

import java.util.Map;

/** The request is well-formed but breaks a business rule. Mapped to 422. */
public abstract class BusinessRuleException extends DomainException {

    protected BusinessRuleException(String code, String message, Map<String, Object> properties) {
        super(code, message, properties);
    }
}
