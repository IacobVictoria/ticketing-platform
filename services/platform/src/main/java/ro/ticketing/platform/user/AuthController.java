package ro.ticketing.platform.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ro.ticketing.platform.security.IssuedToken;
import ro.ticketing.platform.security.TokenService;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final UserService userService;
    private final TokenService tokenService;

    AuthController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.email(), request.password(), request.firstName(),
                request.lastName());
        return UserResponse.from(user);
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request.email(), request.password());
        IssuedToken token = tokenService.issue(user.getId(), user.getRole().name());
        return LoginResponse.of(token, user);
    }
}
