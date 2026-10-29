package ro.ticketing.platform.user;

import java.time.Instant;
import java.util.UUID;

/** Never contains the password hash. */
record UserResponse(UUID id, String email, String firstName, String lastName, UserRole role, Instant createdAt) {

    static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getRole(), user.getCreatedAt());
    }
}
