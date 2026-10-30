package ro.ticketing.platform.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Runs once at startup: creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD if there is no admin yet. */
@Component
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserService userService;
    private final String email;
    private final String password;

    AdminBootstrap(UserService userService,
            @Value("${app.admin.email}") String email,
            @Value("${app.admin.password}") String password) {
        this.userService = userService;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userService.adminExists()) {
            return;
        }
        // never turn an existing account into an admin: without email verification, anyone could have registered it
        if (userService.emailExists(email)) {
            throw new IllegalStateException("ADMIN_EMAIL " + email + " already belongs to a normal account");
        }
        userService.createAdmin(email, password, "Admin", "Platforma");
        log.info("Created the first admin account: {}", email);
    }
}
