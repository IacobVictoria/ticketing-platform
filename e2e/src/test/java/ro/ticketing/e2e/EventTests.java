package ro.ticketing.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** The public event pages, seen by a visitor who is not logged in. */
@UsePlaywright(AppOptions.class)
class EventTests {

    @Test
    void publishedEventAppearsInTheListWithItsDetails(Page page, APIRequestContext request) {
        String name = "Concert E2E " + System.nanoTime();
        TestApi.createEvent(request, name, true);

        page.navigate("/");
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name)).click();

        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(name))).isVisible();
        assertThat(page.getByText("Str. Testelor 1, Cluj-Napoca")).isVisible();

        // one table row per sector: name, type and number of places
        Locator standing = page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("Gazon"));
        assertThat(standing).containsText("În picioare");
        assertThat(standing).containsText("5000");

        Locator seated = page.getByRole(AriaRole.ROW, new Page.GetByRoleOptions().setName("Tribuna 1"));
        assertThat(seated).containsText("Locuri numerotate");
        assertThat(seated).containsText("20");

        assertThat(page.getByText("Capacitate totală: 5020 de locuri")).isVisible();
    }

    @Test
    void draftEventIsHiddenFromVisitors(Page page, APIRequestContext request) {
        String name = "Draft E2E " + System.nanoTime();
        String eventId = TestApi.createEvent(request, name, false);

        page.navigate("/");
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Evenimente"))).isVisible();
        assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(name))).hasCount(0);

        // even with the direct link the backend answers 404 for a draft
        page.navigate("/events/" + eventId);
        assertThat(page.getByRole(AriaRole.HEADING)).hasText("Evenimentul nu există");
    }

    @Test
    void unknownEventShowsNotFound(Page page) {
        page.navigate("/events/00000000-0000-0000-0000-000000000000");

        assertThat(page.getByRole(AriaRole.HEADING)).hasText("Evenimentul nu există");
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("← Toate evenimentele")).click();
        assertThat(page).hasURL("http://localhost:4200/");
    }
}
