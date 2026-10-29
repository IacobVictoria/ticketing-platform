package ro.ticketing.platform.user;

import ro.ticketing.platform.error.ConflictException;

import java.util.Map;

class EmailAlreadyRegisteredException extends ConflictException {

    EmailAlreadyRegisteredException(String email) {
        super("EMAIL_ALREADY_REGISTERED", "An account with this email already exists", Map.of("email", email));
    }
}
