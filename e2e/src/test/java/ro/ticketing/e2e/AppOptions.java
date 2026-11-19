package ro.ticketing.e2e;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;

/** Settings shared by every test class: where the app is and whether we see the browser window. */
public class AppOptions implements OptionsFactory {

    @Override
    public Options getOptions() {
        return new Options()
                .setBaseUrl("http://localhost:4200")
                // mvnw test -Dheaded  -> opens a real Chrome window, useful while writing a test
                .setHeadless(System.getProperty("headed") == null);
    }
}
