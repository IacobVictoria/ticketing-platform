package ro.ticketing.platform.user;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** There is no role field: every registered account is a USER, whatever the client sends. */
record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank String confirmPassword,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName
) {

    /** Trim before validation runs, so " ana@x.ro " is a valid email. Passwords are kept exactly as typed. */
    RegisterRequest {
        email = strip(email);
        firstName = strip(firstName);
        lastName = strip(lastName);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    /** Validated like a field; reported as "passwordConfirmed" in the error list. Never stored. */
    @AssertTrue(message = "passwords do not match")
    boolean isPasswordConfirmed() {
        return password == null || password.equals(confirmPassword);
    }
}
