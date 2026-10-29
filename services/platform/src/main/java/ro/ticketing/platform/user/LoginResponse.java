package ro.ticketing.platform.user;

import ro.ticketing.platform.security.IssuedToken;

/**
 * The token is for the backend (sent back as "Authorization: Bearer ..."),
 * the user (with the role) is for the frontend to decide what to show.
 */
record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    static LoginResponse of(IssuedToken token, User user) {
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(), UserResponse.from(user));
    }
}
