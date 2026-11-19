package ro.ticketing.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Browser tests for the login page. They need the whole app running:
 * Docker (Postgres + Mailpit), platform on 8081 and Angular (ng serve) on 4200.
 */
@UsePlaywright(AppOptions.class)
class LoginTests {

    @Test
    void adminCanLogIn(Page page) {
        page.navigate("/login");

        page.getByLabel("Email").fill(TestApi.ADMIN_EMAIL);
        page.getByLabel("Parolă").fill(TestApi.ADMIN_PASSWORD);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Intră în cont")).click();

        // after login we land on the event list and the header shows the user's first name
        assertThat(page).hasURL("http://localhost:4200/");
        assertThat(page.getByText("Admin", new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ieși din cont"))).isVisible();
    }

    @Test
    void wrongPasswordShowsAnError(Page page) {
        page.navigate("/login");

        page.getByLabel("Email").fill(TestApi.ADMIN_EMAIL);
        page.getByLabel("Parolă").fill("parola-gresita");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Intră în cont")).click();

        assertThat(page.getByRole(AriaRole.ALERT)).hasText("Email sau parolă greșită.");
        assertThat(page).hasURL("http://localhost:4200/login");
    }

    @Test
    void emptyFormShowsAnError(Page page) {
        page.navigate("/login");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Intră în cont")).click();

        assertThat(page.getByRole(AriaRole.ALERT)).hasText("Completează emailul și parola.");
    }

    @Test
    void unverifiedAccountCannotLogIn(Page page, APIRequestContext request) {
        String email = TestApi.uniqueEmail();
        TestApi.register(request, email, "parola-buna-123", "Ana");

        page.navigate("/login");
        page.getByLabel("Email").fill(email);
        page.getByLabel("Parolă").fill("parola-buna-123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Intră în cont")).click();

        // here the page has two role="alert" elements (ours + the empty one of the resend button),
        // so we look for the text instead of the role
        assertThat(page.getByText("Confirmă-ți adresa de email folosind linkul primit.")).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Retrimite emailul de confirmare"))).isVisible();
    }
}
