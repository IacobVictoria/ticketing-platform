package ro.ticketing.e2e;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * The access token lives only in Angular's memory, so a page reload loses it.
 * These tests check that the refresh-token cookie brings the session back (and that logout really ends it).
 */
@UsePlaywright(AppOptions.class)
class SessionTests {

    @Test
    void sessionSurvivesAPageReload(Page page) {
        logInAsAdmin(page);

        page.reload();

        // provideAppInitializer called /api/auth/refresh with the HttpOnly cookie before showing the page
        assertThat(page.getByText("Admin", new Page.GetByTextOptions().setExact(true))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ieși din cont"))).isVisible();
    }

    @Test
    void logoutEndsTheSession(Page page) {
        logInAsAdmin(page);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ieși din cont")).click();
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Intră în cont"))).isVisible();

        // after a reload we must still be logged out: the refresh token was revoked
        page.reload();
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Intră în cont"))).isVisible();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ieși din cont"))).isHidden();
    }

    private void logInAsAdmin(Page page) {
        page.navigate("/login");
        page.getByLabel("Email").fill(TestApi.ADMIN_EMAIL);
        page.getByLabel("Parolă").fill(TestApi.ADMIN_PASSWORD);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Intră în cont")).click();
        assertThat(page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Ieși din cont"))).isVisible();
    }
}
