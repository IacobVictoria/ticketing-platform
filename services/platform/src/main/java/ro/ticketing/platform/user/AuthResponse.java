package ro.ticketing.platform.user;

import ro.ticketing.platform.security.IssuedToken;

/**
 * Returned by login and refresh. The access token is for the backend (sent back as "Authorization: Bearer ...")
 * and lives only in the frontend's memory; the user (with the role) is for the frontend to decide what to show.
 * The refresh token is not here: it travels only in the HttpOnly cookie.
 */
record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    static AuthResponse of(IssuedToken token, User user) {
        return new AuthResponse(token.value(), "Bearer", token.expiresInSeconds(), UserResponse.from(user));
    }
}
