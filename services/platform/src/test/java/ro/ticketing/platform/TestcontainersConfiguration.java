package ro.ticketing.platform;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;

/** What every test needs: the database, and a fake mail sender so that tests never send real emails. */
@TestConfiguration(proxyBeanMethods = false)
@Import(PostgresContainerConfiguration.class)
public class TestcontainersConfiguration {

    @Bean
    JavaMailSender mailSender() {
        return Mockito.mock(JavaMailSender.class);
    }

}
