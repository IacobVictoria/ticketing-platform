package ro.ticketing.platform.user;

import jakarta.validation.constraints.NotBlank;

/** No format or length rules: a wrong email or password is simply "invalid credentials". */
record LoginRequest(@NotBlank String email, @NotBlank String password) {

    LoginRequest {
        email = email == null ? null : email.strip();
    }
}
