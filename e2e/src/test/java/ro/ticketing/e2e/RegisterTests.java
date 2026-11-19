package ro.ticketing.e2e;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Register -> email in Mailpit -> confirm with the password -> log in. */
@UsePlaywright(AppOptions.class)
class RegisterTests {

    @Test
    void newUserRegistersConfirmsEmailAndLogsIn(Page page, APIRequestContext request) {
        String email = TestApi.uniqueEmail();

        // 1. fill the register form
        page.navigate("/register");
        page.getByLabel("Prenume").fill("Ioana");
        page.getByLabel("Nume", new Page.GetByLabelOptions().setExact(true)).fill("Popescu");
        page.getByLabel("Email").fill(email);
        page.getByLabel("Parolă (minimum 8 caractere)").fill("parola-buna-123");
        page.getByLabel("Confirmă parola").fill("parola-buna-123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Creează contul")).click();

        assertThat(page.getByText("Ți-am trimis un email la")).containsText(email);

        // 2. open the link from the email, like the user would
        page.navigate(TestApi.verificationLink(request, email));
        page.getByLabel("Parolă").fill("parola-buna-123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmă contul")).click();

        assertThat(page.getByRole(AriaRole.STATUS)).hasText("Contul tău este activ.");

        // 3. the confirmed account can log in
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Intră în cont").setExact(true))
                .last().click();
        page.getByLabel("Email").fill(email);
        page.getByLabel("Parolă").fill("parola-buna-123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Intră în cont")).click();

        assertThat(page).hasURL("http://localhost:4200/");
        assertThat(page.getByText("Ioana", new Page.GetByTextOptions().setExact(true))).isVisible();
    }

    @Test
    void differentPasswordsShowAnError(Page page) {
        page.navigate("/register");
        page.getByLabel("Prenume").fill("Ioana");
        page.getByLabel("Nume", new Page.GetByLabelOptions().setExact(true)).fill("Popescu");
        page.getByLabel("Email").fill(TestApi.uniqueEmail());
        page.getByLabel("Parolă (minimum 8 caractere)").fill("parola-buna-123");
        page.getByLabel("Confirmă parola").fill("alta-parola-123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Creează contul")).click();

        assertThat(page.getByRole(AriaRole.ALERT)).containsText("identică în ambele câmpuri");
    }

    @Test
    void confirmingWithAnotherPasswordIsRejected(Page page, APIRequestContext request) {
        String email = TestApi.uniqueEmail();
        TestApi.register(request, email, "parola-buna-123", "Ana");

        page.navigate(TestApi.verificationLink(request, email));
        page.getByLabel("Parolă").fill("parola-altcuiva");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmă contul")).click();

        assertThat(page.getByRole(AriaRole.ALERT)).containsText("Parola nu este cea a contului.");
    }

    @Test
    void invalidLinkOffersANewEmail(Page page) {
        page.navigate("/verify-email?token=token-inventat");
        page.getByLabel("Parolă").fill("orice-parola-123");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirmă contul")).click();

        assertThat(page.getByRole(AriaRole.ALERT)).containsText("Linkul nu mai este valabil.");
        assertThat(page.getByLabel("Email")).isVisible();
    }
}
