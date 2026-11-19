package ro.ticketing.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Prepares test data straight through the API, so each test only clicks through the part it checks.
 * Paths starting with /api go through the Angular proxy (localhost:4200 -> 8081).
 */
class TestApi {

    static final String ADMIN_EMAIL = "admin@ticketing.local";
    static final String ADMIN_PASSWORD = "admin-local-password";

    private static final String MAILPIT = "http://localhost:8025";

    /** A new address on every run, so tests never collide with accounts from earlier runs. */
    static String uniqueEmail() {
        return "e2e-" + System.nanoTime() + "@test.local";
    }

    static void register(APIRequestContext request, String email, String password, String firstName) {
        APIResponse response = request.post("/api/auth/register", RequestOptions.create().setData(Map.of(
                "email", email,
                "password", password,
                "confirmPassword", password,
                "firstName", firstName,
                "lastName", "Test")));
        assertEquals(202, response.status());
    }

    /**
     * Reads the confirmation link from the email in Mailpit.
     * The email leaves in the background (@Async), so we ask a few times before giving up.
     */
    static String verificationLink(APIRequestContext request, String email) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String search = request.get(MAILPIT + "/api/v1/search",
                    RequestOptions.create().setQueryParam("query", "to:" + email)).text();
            String messageId = find(search, "\"ID\"\\s*:\\s*\"([^\"]+)\"");
            if (messageId != null) {
                String message = request.get(MAILPIT + "/api/v1/message/" + messageId).text();
                return find(message, "(http://localhost:4200/verify-email\\?token=[A-Za-z0-9_-]+)");
            }
            sleep(500);
        }
        fail("No verification email arrived for " + email);
        return null;
    }

    /** Creates a venue (one standing + one seated sector) and an event there. Returns the event id. */
    static String createEvent(APIRequestContext request, String eventName, boolean publish) {
        String token = adminToken(request);

        String venueId = idFrom(request.post("/api/venues", asAdmin(token)
                .setData(Map.of("name", "Sala E2E", "city", "Cluj-Napoca", "address", "Str. Testelor 1"))));

        idFrom(request.post("/api/venues/" + venueId + "/sectors", asAdmin(token)
                .setData(Map.of("name", "Gazon", "kind", "STANDING", "capacity", 5000))));
        String seatedId = idFrom(request.post("/api/venues/" + venueId + "/sectors", asAdmin(token)
                .setData(Map.of("name", "Tribuna 1", "kind", "SEATED"))));
        idFrom(request.post("/api/venues/" + venueId + "/sectors/" + seatedId + "/rows", asAdmin(token)
                .setData(Map.of("rowLabel", "A", "seats", 20))));

        String startsAt = Instant.now().plus(30, ChronoUnit.DAYS).toString();
        String eventId = idFrom(request.post("/api/events", asAdmin(token)
                .setData(Map.of("name", eventName, "description", "Creat de testele E2E",
                        "startsAt", startsAt, "venueId", venueId))));

        if (publish) {
            APIResponse published = request.post("/api/events/" + eventId + "/publish", asAdmin(token));
            assertEquals(200, published.status(), published.text());
        }
        return eventId;
    }

    /** Request options with the admin's access token in the Authorization header. */
    private static RequestOptions asAdmin(String token) {
        return RequestOptions.create().setHeader("Authorization", "Bearer " + token);
    }

    private static String adminToken(APIRequestContext request) {
        APIResponse response = request.post("/api/auth/login", RequestOptions.create()
                .setData(Map.of("email", ADMIN_EMAIL, "password", ADMIN_PASSWORD)));
        assertEquals(200, response.status());
        return find(response.text(), "\"accessToken\"\\s*:\\s*\"([^\"]+)\"");
    }

    /** The first "id" in a JSON response is the id of the created object. */
    private static String idFrom(APIResponse response) {
        assertEquals(201, response.status(), response.text());
        return find(response.text(), "\"id\"\\s*:\\s*\"([^\"]+)\"");
    }

    /** Returns the first group of the regex, or null when nothing matches. */
    private static String find(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
