package ro.ticketing.platform.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.ticketing.platform.security.IssuedToken;
import ro.ticketing.platform.security.TokenService;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookies refreshTokenCookies;

    AuthController(UserService userService, TokenService tokenService, RefreshTokenService refreshTokenService,
            RefreshTokenCookies refreshTokenCookies) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenCookies = refreshTokenCookies;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.email(), request.password(), request.firstName(),
                request.lastName());
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.email(), request.password());
        return authenticated(user, refreshTokenService.issueForLogin(user.getId()));
    }

    /**
     * Called by the frontend when the access token expires, and on page load (the access token lived only in memory).
     * The old refresh token is used up; the response carries a new one in the cookie.
     */
    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new InvalidRefreshTokenException();
        }
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken);
        User user = userService.findById(rotation.userId()).orElseThrow(InvalidRefreshTokenException::new);
        return authenticated(user, rotation.refreshToken());
    }

    /** Always 204 and always clears the cookie, even without a valid token: the outcome is "logged out" either way. */
    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = RefreshTokenCookies.NAME, required = false) String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.clear().toString())
                .build();
    }

    /**
     * The current user, from the access token. @AuthenticationPrincipal hands us the already verified JWT;
     * its "sub" is the user id. The route requires authentication, so the token is never null here.
     */
    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return userService.findById(userId).map(UserResponse::from).orElseThrow(AccountNotFoundException::new);
    }

    private ResponseEntity<AuthResponse> authenticated(User user, IssuedRefreshToken refreshToken) {
        IssuedToken accessToken = tokenService.issue(user.getId(), user.getRole().name());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookies.create(refreshToken).toString())
                .body(AuthResponse.of(accessToken, user));
    }
}
